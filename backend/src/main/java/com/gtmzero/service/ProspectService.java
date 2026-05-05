package com.gtmzero.service;

import com.gtmzero.dto.outreach.GenerateOutreachRequest;
import com.gtmzero.dto.outreach.ProspectSummaryDto;
import com.gtmzero.entity.Prospect;
import com.gtmzero.repository.OutreachMessageRepository;
import com.gtmzero.repository.ProspectRepository;
import com.gtmzero.service.ProspectUrlSignalExtractor.ExtractedSignals;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Find-or-create + read-side projection for {@link Prospect}.
 *
 * <p>Dedup priority on write:
 * <ol>
 *   <li>linkedinUrl (exact match)</li>
 *   <li>githubUrl (exact match)</li>
 *   <li>otherwise → create a new row, no name+company dedup in MVP</li>
 * </ol>
 *
 * <p>When a prospect already exists, fields are only filled in if currently
 * null/empty — never overwritten. A re-submission with partial info therefore
 * cannot accidentally wipe richer existing data.
 */
@Service
@Slf4j
public class ProspectService {

    private final ProspectRepository prospectRepository;
    private final OutreachMessageRepository outreachMessageRepository;
    private final ProspectUrlSignalExtractor signalExtractor;

    public ProspectService(ProspectRepository prospectRepository,
                           OutreachMessageRepository outreachMessageRepository,
                           ProspectUrlSignalExtractor signalExtractor) {
        this.prospectRepository = prospectRepository;
        this.outreachMessageRepository = outreachMessageRepository;
        this.signalExtractor = signalExtractor;
    }

    @Transactional
    public Prospect findOrCreate(GenerateOutreachRequest request) {
        ExtractedSignals signals = signalExtractor.extract(request.linkedinUrl(), request.githubUrl());

        Optional<Prospect> existing = Optional.empty();
        if (request.linkedinUrl() != null && !request.linkedinUrl().isBlank()) {
            existing = prospectRepository.findByLinkedinUrl(request.linkedinUrl());
        }
        if (existing.isEmpty() && request.githubUrl() != null && !request.githubUrl().isBlank()) {
            existing = prospectRepository.findByGithubUrl(request.githubUrl());
        }

        if (existing.isPresent()) {
            Prospect p = existing.get();
            boolean changed = false;

            if (isBlank(p.getFullName()) && !isBlank(request.fullName())) {
                p.setFullName(request.fullName()); changed = true;
            }
            if (isBlank(p.getRole()) && !isBlank(request.role())) {
                p.setRole(request.role()); changed = true;
            }
            if (isBlank(p.getCompanyDomain()) && !isBlank(request.companyDomain())) {
                p.setCompanyDomain(request.companyDomain()); changed = true;
            }
            if (isBlank(p.getLinkedinUrl()) && !isBlank(request.linkedinUrl())) {
                p.setLinkedinUrl(request.linkedinUrl()); changed = true;
            }
            if (isBlank(p.getGithubUrl()) && !isBlank(request.githubUrl())) {
                p.setGithubUrl(request.githubUrl()); changed = true;
            }
            if (p.getTechStackSignals() == null || p.getTechStackSignals().length == 0) {
                String[] merged = mergeSignals(request.techStackSignals(), signals.heuristicSignals());
                if (merged.length > 0) {
                    p.setTechStackSignals(merged); changed = true;
                }
            }
            if (changed) {
                log.info("Prospect {} updated with new fields from re-submission", p.getId());
                return prospectRepository.save(p);
            }
            return p;
        }

        Prospect created = Prospect.builder()
                .fullName(emptyToNull(request.fullName()))
                .role(emptyToNull(request.role()))
                .companyName(request.companyName())
                .companyDomain(emptyToNull(request.companyDomain()))
                .linkedinUrl(emptyToNull(request.linkedinUrl()))
                .githubUrl(emptyToNull(request.githubUrl()))
                .techStackSignals(mergeSignals(request.techStackSignals(), signals.heuristicSignals()))
                .notes(emptyToNull(request.contextNotes()))
                .build();
        Prospect saved = prospectRepository.save(created);
        log.info("Created new Prospect {} for {} @ {}", saved.getId(),
                saved.getFullName() != null ? saved.getFullName() : "(unknown)",
                saved.getCompanyName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProspectSummaryDto> listAll() {
        List<Prospect> prospects = prospectRepository.findAllByOrderByCreatedAtDesc();
        List<ProspectSummaryDto> out = new ArrayList<>(prospects.size());
        for (Prospect p : prospects) {
            int count = (int) outreachMessageRepository.countByProspectId(p.getId());
            out.add(toSummary(p, count));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Optional<ProspectSummaryDto> findById(UUID id) {
        return prospectRepository.findById(id)
                .map(p -> toSummary(p, (int) outreachMessageRepository.countByProspectId(p.getId())));
    }

    public ProspectSummaryDto toSummary(Prospect p, int outreachCount) {
        return new ProspectSummaryDto(
                p.getId(),
                p.getFullName(),
                p.getRole(),
                p.getCompanyName(),
                p.getCompanyDomain(),
                p.getLinkedinUrl(),
                p.getGithubUrl(),
                p.getTechStackSignals() == null ? List.of() : List.of(p.getTechStackSignals()),
                outreachCount,
                p.getCreatedAt()
        );
    }

    private static String[] mergeSignals(List<String> manual, List<String> extracted) {
        Set<String> ordered = new LinkedHashSet<>();
        if (manual != null) for (String s : manual) if (!isBlank(s)) ordered.add(s.trim());
        if (extracted != null) for (String s : extracted) if (!isBlank(s)) ordered.add(s.trim());
        return ordered.toArray(String[]::new);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String emptyToNull(String s) {
        return isBlank(s) ? null : s;
    }
}
