package com.gtmzero.service;

import com.gtmzero.dto.objection.ObjectionRequest;
import com.gtmzero.dto.objection.ObjectionResponse;
import com.gtmzero.dto.objection.ObjectionStreamEvent;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Completed;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Failed;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Retrieved;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Started;
import com.gtmzero.dto.objection.ObjectionStreamEvent.Token;
import com.gtmzero.entity.DocumentChunk;
import com.gtmzero.service.CitationValidator.ValidationResult;
import com.gtmzero.service.PromptBuilder.BuiltPrompt;
import com.gtmzero.service.RetrievalService.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Orchestrates the objection-answering pipeline as a reactive stream of events.
 *
 * <p>Sequence on success: {@code Started → Retrieved → Token* → Completed}.
 * On any failure mid-stream, {@code Failed} is emitted and the stream completes.
 *
 * <p>The pipeline does NOT block the caller's thread — retrieval and the
 * Anthropic call run on Reactor's bounded-elastic / event-loop schedulers.
 * Persistence is dispatched via {@link ObjectionPersistenceService} on the
 * Spring {@code @Async} executor so the SSE Completed event can flush to the
 * client without waiting on database IO.
 */
@Service
@Slf4j
public class ObjectionService {

    private static final int DEFAULT_TOP_K = 4;

    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final CitationValidator citationValidator;
    private final ChatModel chatModel;
    private final ObjectionPersistenceService persistenceService;
    private final String modelName;

    public ObjectionService(RetrievalService retrievalService,
                            PromptBuilder promptBuilder,
                            CitationValidator citationValidator,
                            ChatModel chatModel,
                            ObjectionPersistenceService persistenceService,
                            @Value("${spring.ai.anthropic.chat.options.model:claude-sonnet-4-6}") String modelName) {
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
        this.citationValidator = citationValidator;
        this.chatModel = chatModel;
        this.persistenceService = persistenceService;
        this.modelName = modelName;
    }

    public Flux<ObjectionStreamEvent> handleObjection(ObjectionRequest request) {
        return Flux.defer(() -> {
            final UUID queryId = UUID.randomUUID();
            final long startNanos = System.nanoTime();
            final AtomicLong firstTokenNanos = new AtomicLong(0);
            final StringBuilder fullAnswer = new StringBuilder(2048);

            Flux<ObjectionStreamEvent> startedFlux = Flux.just(
                    new Started(queryId, System.currentTimeMillis()));

            // Retrieval is blocking JDBC + HTTP — keep it off the event-loop.
            Mono<RetrievedContext> retrievalMono = Mono.fromCallable(() -> {
                int topK = Optional.ofNullable(request.topK()).orElse(DEFAULT_TOP_K);
                RetrievalResult retrieval = retrievalService.retrieveTopK(request.question(), topK);
                BuiltPrompt prompt = promptBuilder.build(request.question(), retrieval.chunks());
                return new RetrievedContext(retrieval, prompt);
            }).subscribeOn(Schedulers.boundedElastic());

            Flux<ObjectionStreamEvent> downstream = retrievalMono.flatMapMany(ctx -> {
                Flux<ObjectionStreamEvent> retrievedEvent = Flux.just(
                        new Retrieved(ctx.prompt.citations(), ctx.retrieval.latencyMs()));

                Prompt llmPrompt = new Prompt(List.of(
                        new SystemMessage(ctx.prompt.systemPrompt()),
                        new UserMessage(ctx.prompt.userPrompt())
                ));

                Flux<ObjectionStreamEvent> tokenFlux = chatModel.stream(llmPrompt)
                        .handle((ChatResponse resp, SynchronousSink<ObjectionStreamEvent> sink) -> {
                            if (firstTokenNanos.get() == 0L) {
                                firstTokenNanos.compareAndSet(0L, System.nanoTime());
                            }
                            String text = extractText(resp);
                            if (text != null && !text.isEmpty()) {
                                fullAnswer.append(text);
                                sink.next(new Token(text));
                            }
                        });

                Mono<ObjectionStreamEvent> completionMono = Mono.fromSupplier(() -> {
                    long totalMs = (System.nanoTime() - startNanos) / 1_000_000L;
                    long firstTokenMs = firstTokenNanos.get() == 0L
                            ? totalMs
                            : (firstTokenNanos.get() - startNanos) / 1_000_000L;

                    String answer = fullAnswer.toString();
                    ValidationResult validation = citationValidator.validate(answer, ctx.prompt.citations());

                    ObjectionResponse response = new ObjectionResponse(
                            queryId,
                            request.question(),
                            answer,
                            ctx.prompt.citations(),
                            ctx.retrieval.chunks().size(),
                            firstTokenMs,
                            totalMs,
                            modelName
                    );

                    List<UUID> chunkIds = ctx.retrieval.chunks().stream()
                            .map(DocumentChunk::getId)
                            .toList();

                    // Off the response path — never blocks the SSE flush.
                    persistenceService.persist(queryId, request, response, validation, chunkIds);

                    log.info("Objection {} completed in {}ms (first token at {}ms, {} chunks)",
                            queryId, totalMs, firstTokenMs, ctx.retrieval.chunks().size());

                    return new Completed(response);
                });

                return Flux.concat(retrievedEvent, tokenFlux, completionMono);
            });

            return Flux.concat(startedFlux, downstream)
                    .onErrorResume(err -> {
                        log.error("Objection {} failed", request.sessionId(), err);
                        String msg = err.getMessage() != null ? err.getMessage() : err.getClass().getSimpleName();
                        return Flux.just(new Failed(msg, "STREAM_ERROR"));
                    });
        });
    }

    /**
     * Defensive extraction — Spring AI's streaming responses occasionally carry
     * a null Generation or null content for keep-alive / metadata frames.
     */
    private static String extractText(ChatResponse resp) {
        if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) {
            return null;
        }
        return resp.getResult().getOutput().getText();
    }

    private record RetrievedContext(RetrievalResult retrieval, BuiltPrompt prompt) {}
}
