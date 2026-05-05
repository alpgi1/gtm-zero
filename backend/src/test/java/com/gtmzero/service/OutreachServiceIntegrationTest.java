package com.gtmzero.service;

import com.gtmzero.dto.outreach.GenerateOutreachRequest;
import com.gtmzero.dto.outreach.OutreachResponse;
import com.gtmzero.entity.AuditLog;
import com.gtmzero.entity.OutreachMessage;
import com.gtmzero.entity.Prospect;
import com.gtmzero.exception.OutreachGenerationException;
import com.gtmzero.repository.AuditLogRepository;
import com.gtmzero.repository.OutreachMessageRepository;
import com.gtmzero.repository.ProspectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Full pipeline against the real database with the ChatModel mocked.
 *
 * <p>Not transactional — we want to inspect actual rows after each call.
 * Cleans up tables before and after each test.
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.outreach-seed.enabled=false")
class OutreachServiceIntegrationTest {

    @Autowired private OutreachService outreachService;
    @Autowired private ProspectRepository prospectRepository;
    @Autowired private OutreachMessageRepository outreachMessageRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @MockitoBean private ChatModel chatModel;

    private static final String VALID_JSON = """
            {
              "subject": "Quick AI Act question",
              "body": "Hi Alice,\\n\\nSaw Acme just shipped its analytics dashboard. \
            With the EU AI Act enforcement window opening in August 2026, \
            classification work for ML-driven products is the kind of thing \
            that bites founders late. Regu does first-pass risk classification \
            with article-level citations — useful as a sanity check before \
            you spend on outside counsel.\\n\\n15-minute call next week, or one \
            line back if you're already on top of it?\\n\\nAlpgiray",
              "personalization_basis": "Their product is ML-driven analytics, derived from contextNotes."
            }
            """;

    @BeforeEach
    void setUp() {
        cleanup();
        // Default stub returns the deterministic envelope.
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(chatResponse(VALID_JSON));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        outreachMessageRepository.deleteAll();
        prospectRepository.deleteAll();
        auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc("OUTREACH_GENERATED")
                .forEach(auditLogRepository::delete);
        auditLogRepository.findAllByEventTypeOrderByCreatedAtDesc("OUTREACH_SENT_MOCK")
                .forEach(auditLogRepository::delete);
    }

    @Test
    void generate_createsProspectMessageAndAuditLog() {
        GenerateOutreachRequest req = sampleRequest("https://linkedin.com/in/alice-smith");

        OutreachResponse resp = outreachService.generate(req);

        assertThat(resp.outreachId()).isNotNull();
        assertThat(resp.prospectId()).isNotNull();
        assertThat(resp.subject()).isEqualTo("Quick AI Act question");
        assertThat(resp.body()).contains("Alpgiray");
        assertThat(resp.model()).isNotBlank();
        assertThat(resp.generationPromptVersion()).isEqualTo(OutreachPromptBuilder.PROMPT_VERSION);
        assertThat(resp.status()).isEqualTo("GENERATED");

        Prospect saved = prospectRepository.findById(resp.prospectId()).orElseThrow();
        assertThat(saved.getCompanyName()).isEqualTo("Acme AI");
        assertThat(saved.getLinkedinUrl()).isEqualTo("https://linkedin.com/in/alice-smith");
        assertThat(saved.getTechStackSignals()).contains("Python", "linkedin-profile-public");

        OutreachMessage msg = outreachMessageRepository.findById(resp.outreachId()).orElseThrow();
        assertThat(msg.getProspect().getId()).isEqualTo(saved.getId());

        List<AuditLog> audits = auditLogRepository
                .findAllByEventTypeOrderByCreatedAtDesc("OUTREACH_GENERATED");
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getSummary()).contains("Acme AI");
    }

    @Test
    void generate_dedupsProspectByLinkedinUrl() {
        GenerateOutreachRequest req = sampleRequest("https://linkedin.com/in/alice-smith");

        OutreachResponse first = outreachService.generate(req);
        OutreachResponse second = outreachService.generate(req);

        assertThat(second.prospectId()).isEqualTo(first.prospectId());
        assertThat(second.outreachId()).isNotEqualTo(first.outreachId());
        assertThat(prospectRepository.count()).isEqualTo(1);
        assertThat(outreachMessageRepository.count()).isEqualTo(2);
    }

    @Test
    void generate_retriesOnceWhenJsonInvalid_thenFails() {
        AtomicInteger calls = new AtomicInteger(0);
        when(chatModel.call(any(Prompt.class))).thenAnswer(inv -> {
            calls.incrementAndGet();
            return chatResponse("this is not json at all, sorry");
        });

        GenerateOutreachRequest req = sampleRequest("https://linkedin.com/in/bob-jones");

        assertThatThrownBy(() -> outreachService.generate(req))
                .isInstanceOf(OutreachGenerationException.class)
                .matches(e -> ((OutreachGenerationException) e).getCode()
                        .equals(OutreachGenerationException.OUTPUT_FORMAT_INVALID));
        assertThat(calls.get()).isEqualTo(2);
        // The prospect was created in the same @Transactional method that threw —
        // Spring marks the transaction rollback-only on RuntimeException, so we
        // expect zero rows persisted on this failure path.
        assertThat(outreachMessageRepository.count()).isZero();
    }

    @Test
    void generate_recoversWhenFirstCallReturnsBadJsonButRetrySucceeds() {
        AtomicInteger calls = new AtomicInteger(0);
        when(chatModel.call(any(Prompt.class))).thenAnswer(inv ->
                calls.incrementAndGet() == 1
                        ? chatResponse("not json")
                        : chatResponse(VALID_JSON));

        GenerateOutreachRequest req = sampleRequest("https://linkedin.com/in/charlie-day");

        OutreachResponse resp = outreachService.generate(req);
        assertThat(resp.subject()).isEqualTo("Quick AI Act question");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void markAsSent_updatesStatusAndInsertsAudit() {
        GenerateOutreachRequest req = sampleRequest("https://linkedin.com/in/dana-park");
        OutreachResponse generated = outreachService.generate(req);

        OutreachResponse sent = outreachService.markAsSent(generated.outreachId());
        assertThat(sent.status()).isEqualTo("SENT_MOCK");

        OutreachMessage row = outreachMessageRepository.findById(generated.outreachId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo("SENT_MOCK");

        List<AuditLog> audits = auditLogRepository
                .findAllByEventTypeOrderByCreatedAtDesc("OUTREACH_SENT_MOCK");
        assertThat(audits).hasSize(1);
    }

    @Test
    void generate_rejectsRequestWithoutMinimumIdentity() {
        GenerateOutreachRequest req = new GenerateOutreachRequest(
                null, null, "Acme AI", "acme.ai",
                null, null, List.of(), null);
        assertThatThrownBy(() -> outreachService.generate(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generate_stripsCodeFences() {
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(
                "```json\n" + VALID_JSON + "\n```"));
        GenerateOutreachRequest req = sampleRequest("https://linkedin.com/in/eric-fence");
        OutreachResponse resp = outreachService.generate(req);
        assertThat(resp.subject()).isEqualTo("Quick AI Act question");
    }

    private static GenerateOutreachRequest sampleRequest(String linkedinUrl) {
        return new GenerateOutreachRequest(
                "Alice Smith",
                "Head of AI",
                "Acme AI",
                "acme.ai",
                linkedinUrl,
                null,
                List.of("Python", "PyTorch"),
                "Just shipped an ML-driven analytics dashboard."
        );
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }
}
