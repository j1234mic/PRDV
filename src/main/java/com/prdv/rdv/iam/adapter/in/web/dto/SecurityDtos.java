package com.prdv.rdv.iam.adapter.in.web.dto;

import com.prdv.rdv.iam.domain.model.security.BugBountyReport;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class SecurityDtos {

    private SecurityDtos() {}

    public record RegisterE2eeKeyRequest(
            String keyId,
            String algorithm,
            @NotBlank String publicKeyPem
    ) {}

    public record SealMedicalPayloadRequest(
            @NotNull Long recipientUserId,
            @NotBlank String plainPayload
    ) {}

    public record OpenMedicalPayloadRequest(
            @NotBlank String cipherText
    ) {}

    public record DecryptedMedicalPayloadResponse(
            String plainPayload
    ) {}

    public record RecordPentestReportRequest(
            @NotBlank String campaignName,
            String auditor,
            String scope,
            int criticalCount,
            int highCount,
            int mediumCount,
            int lowCount,
            String summary,
            List<String> findings,
            Instant nextScheduledAt
    ) {}

    public record SubmitBugBountyReportRequest(
            @NotBlank @Email String reporterEmail,
            String reporterAlias,
            @NotBlank String title,
            @NotBlank String description,
            String affectedComponent,
            BugBountyReport.Severity severity,
            Double cvssScore
    ) {}

    public record TriageBugBountyReportRequest(
            @NotNull BugBountyReport.Status status,
            long bountyAmountCents,
            String resolutionNote
    ) {}
}
