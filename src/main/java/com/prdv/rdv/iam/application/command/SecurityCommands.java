package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.security.BugBountyReport;

import java.time.Instant;
import java.util.List;

/**
 * Commandes de securite avancee : chiffrement E2EE medical, campagnes de
 * tests d'intrusion (pentest) et programme de Bug Bounty.
 */
public final class SecurityCommands {

    private SecurityCommands() {
    }

    public record RegisterE2eeKey(String keyId, String algorithm, String publicKeyPem) {
    }

    public record SealMedicalPayload(Long recipientUserId, String plainPayload) {
    }

    public record OpenMedicalPayload(String cipherText) {
    }

    public record RecordPentestReport(String campaignName,
                                      String auditor,
                                      String scope,
                                      int criticalCount,
                                      int highCount,
                                      int mediumCount,
                                      int lowCount,
                                      String summary,
                                      List<String> findings,
                                      Instant nextScheduledAt) {
    }

    public record SubmitBugBountyReport(String reporterEmail,
                                        String reporterAlias,
                                        String title,
                                        String description,
                                        String affectedComponent,
                                        BugBountyReport.Severity severity,
                                        Double cvssScore) {
    }

    public record TriageBugBountyReport(Long reportId,
                                        BugBountyReport.Status status,
                                        long bountyAmountCents,
                                        String resolutionNote) {
    }
}
