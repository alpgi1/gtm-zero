package com.gtmzero.dto.outreach;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Input for the outreach-generation pipeline.
 *
 * <p>The caller must supply at least one of: a LinkedIn URL, a GitHub URL,
 * or both a fullName and a role — see {@link #hasMinimumIdentity()}. We
 * keep the rule outside Bean Validation because expressing "any one of
 * three" via {@code @AssertTrue} on a record is awkward; the service
 * checks it explicitly.
 */
public record GenerateOutreachRequest(

        @Size(max = 255)
        String fullName,

        @Size(max = 255)
        String role,

        @NotBlank
        @Size(max = 255)
        String companyName,

        @Size(max = 255)
        String companyDomain,

        String linkedinUrl,

        String githubUrl,

        List<String> techStackSignals,

        @Size(max = 1000)
        String contextNotes
) {
    public boolean hasMinimumIdentity() {
        boolean hasUrl = (linkedinUrl != null && !linkedinUrl.isBlank())
                || (githubUrl != null && !githubUrl.isBlank());
        boolean hasNameAndRole = fullName != null && !fullName.isBlank()
                && role != null && !role.isBlank();
        return hasUrl || hasNameAndRole;
    }
}
