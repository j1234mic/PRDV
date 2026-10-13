package com.prdv.rdv.iam.domain.model.security;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Signalement de vulnerabilite dans le cadre du programme de Bug Bounty
 * (divulgation responsable).
 */
@Getter
@Setter
public class BugBountyReport {

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Status {
        SUBMITTED,
        TRIAGED,
        ACCEPTED,
        REWARDED,
        DUPLICATE,
        REJECTED,
        RESOLVED
    }

    private Long id;
    private String reporterEmail;
    private String reporterAlias;
    private String title;
    private String description;
    private String affectedComponent;
    private Severity severity;
    private Double cvssScore;
    private Status status;
    private long bountyAmountCents;
    private String resolutionNote;
    private Instant submittedAt;
    private Instant resolvedAt;

    public static BugBountyReport submit(String reporterEmail, String reporterAlias,
                                         String title, String description,
                                         String affectedComponent, Severity severity,
                                         Double cvssScore, Clock clock) {
        if (title == null || title.isBlank() || description == null || description.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le titre et la description de la vulnerabilite sont obligatoires");
        }
        if (cvssScore != null && (cvssScore < 0.0 || cvssScore > 10.0)) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le score CVSS doit etre compris entre 0.0 et 10.0");
        }
        BugBountyReport report = new BugBountyReport();
        report.reporterEmail = reporterEmail == null ? null : reporterEmail.trim().toLowerCase();
        report.reporterAlias = (reporterAlias == null || reporterAlias.isBlank())
                ? "anonymous-researcher" : reporterAlias.trim();
        report.title = title.trim();
        report.description = description.trim();
        report.affectedComponent = (affectedComponent == null || affectedComponent.isBlank())
                ? "iam" : affectedComponent.trim();
        report.severity = severity != null ? severity : Severity.MEDIUM;
        report.cvssScore = cvssScore;
        report.status = Status.SUBMITTED;
        report.bountyAmountCents = 0L;
        report.submittedAt = clock.instant();
        return report;
    }

    public void triage(Status newStatus, long bountyAmountCents, String note, Clock clock) {
        if (newStatus == null) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Le nouveau statut de triage est obligatoire");
        }
        if (bountyAmountCents < 0) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Le montant de la prime ne peut pas etre negatif");
        }
        this.status = newStatus;
        this.bountyAmountCents = bountyAmountCents;
        this.resolutionNote = note;
        if (newStatus == Status.RESOLVED || newStatus == Status.REWARDED
                || newStatus == Status.REJECTED || newStatus == Status.DUPLICATE) {
            this.resolvedAt = clock.instant();
        }
    }
}
