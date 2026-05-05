package com.gtmzero.seed;

import com.gtmzero.dto.outreach.GenerateOutreachRequest;
import com.gtmzero.dto.outreach.OutreachResponse;
import com.gtmzero.entity.OutreachMessage;
import com.gtmzero.entity.Prospect;
import com.gtmzero.repository.OutreachMessageRepository;
import com.gtmzero.repository.ProspectRepository;
import com.gtmzero.service.OutreachService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * One-shot seeder for the three demo prospects shown in the dashboard
 * "Recent Activity" feed at pitch time.
 *
 * <p>The outreach text is NOT hardcoded — we feed each prospect through the
 * real {@link OutreachService} on first startup so the demo always shows
 * authentic LLM output. Subsequent boots find existing rows and skip the
 * call. First-ever startup pays roughly 15-20s for three live LLM calls.
 *
 * <p>{@code @Order(20)} keeps this after document seeding (Order 10 in
 * {@link ReguSeedData}) — though we don't actually depend on documents,
 * a stable startup ordering makes log output easier to read.
 */
@Component
@Order(20)
@Slf4j
public class OutreachSeedData implements CommandLineRunner {

    private final OutreachMessageRepository outreachMessageRepository;
    private final ProspectRepository prospectRepository;
    private final OutreachService outreachService;
    private final boolean enabled;

    public OutreachSeedData(OutreachMessageRepository outreachMessageRepository,
                            ProspectRepository prospectRepository,
                            OutreachService outreachService,
                            @Value("${app.outreach-seed.enabled:true}") boolean enabled) {
        this.outreachMessageRepository = outreachMessageRepository;
        this.prospectRepository = prospectRepository;
        this.outreachService = outreachService;
        this.enabled = enabled;
    }

    /**
     * Wipes the three demo prospects (matched by company name) and their
     * outreach messages, then regenerates via the live LLM. Returns the
     * three regenerated outreach summaries for inspection.
     */
    @Transactional
    public List<OutreachResponse> reseed() {
        List<GenerateOutreachRequest> seeds = seeds();
        List<String> seedCompanies = seeds.stream().map(GenerateOutreachRequest::companyName).toList();

        List<Prospect> existing = prospectRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> seedCompanies.contains(p.getCompanyName()))
                .toList();
        for (Prospect p : existing) {
            List<OutreachMessage> msgs = outreachMessageRepository.findAllByProspectIdOrderByCreatedAtDesc(p.getId());
            outreachMessageRepository.deleteAll(msgs);
            prospectRepository.delete(p);
        }
        log.info("Reseed: wiped {} demo prospects ({})", existing.size(), seedCompanies);

        List<OutreachResponse> regenerated = new ArrayList<>();
        for (GenerateOutreachRequest req : seeds) {
            try {
                OutreachResponse resp = outreachService.generate(req);
                regenerated.add(resp);
                log.info("Reseeded outreach to {} @ {} — outreachId={}, latency={}ms",
                        req.fullName(), req.companyName(), resp.outreachId(), resp.generationLatencyMs());
            } catch (Exception e) {
                log.error("Reseed failed for {} @ {}: {}", req.fullName(), req.companyName(), e.getMessage(), e);
            }
        }
        return regenerated;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("OutreachSeedData disabled via app.outreach-seed.enabled=false.");
            return;
        }
        long existing = outreachMessageRepository.count();
        if (existing > 0) {
            log.info("OutreachSeedData skipped — {} existing OutreachMessage rows.", existing);
            return;
        }

        log.info("OutreachSeedData starting — generating 3 demo outreach messages via real LLM call.");
        long startMs = System.currentTimeMillis();

        for (GenerateOutreachRequest req : seeds()) {
            try {
                var resp = outreachService.generate(req);
                log.info("Seeded outreach to {} @ {} — outreachId={}, latency={}ms",
                        req.fullName(), req.companyName(), resp.outreachId(), resp.generationLatencyMs());
            } catch (Exception e) {
                // Don't crash app startup if Anthropic is unreachable — the demo can
                // run with empty outreach history and the user can re-seed manually.
                log.error("Failed to seed outreach for {} @ {}: {}",
                        req.fullName(), req.companyName(), e.getMessage(), e);
            }
        }
        log.info("OutreachSeedData done in {}ms.", System.currentTimeMillis() - startMs);
    }

    private static List<GenerateOutreachRequest> seeds() {
        return List.of(
                new GenerateOutreachRequest(
                        "Marie Dubois",
                        "VP of Engineering",
                        "Lumeon Health",
                        "lumeon.health",
                        "https://linkedin.com/in/marie-dubois-lumeon",
                        null,
                        List.of("Python", "PyTorch", "FastAPI", "AWS"),
                        "AI clinical decision-support; they just announced FDA submission."
                ),
                new GenerateOutreachRequest(
                        "Tomasz Krawczyk",
                        "CTO",
                        "DataNova GmbH",
                        "datanova.de",
                        "https://linkedin.com/in/tomasz-krawczyk-datanova",
                        "https://github.com/tomasz-krawczyk",
                        List.of("RAG", "LLM", "Postgres", "pgvector", "TypeScript"),
                        "RAG-powered enterprise search, Berlin-based; they raised €4M Series A in April 2026."
                ),
                new GenerateOutreachRequest(
                        "Elena Rossi",
                        "Founding Engineer",
                        "Aero AI",
                        "aero.ai",
                        "https://linkedin.com/in/elena-rossi-aeroai",
                        "https://github.com/elena-rossi",
                        List.of("Go", "Kubernetes", "TimescaleDB", "ML"),
                        "Predictive maintenance for aviation; presented at Slush 2025."
                )
        );
    }
}
