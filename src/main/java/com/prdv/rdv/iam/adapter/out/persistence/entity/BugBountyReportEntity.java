package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.security.BugBountyReport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "bug_bounty_reports")
@Getter
@Setter
public class BugBountyReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String reporterEmail;

    @Column(length = 120)
    private String reporterAlias;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(length = 120)
    private String affectedComponent;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BugBountyReport.Severity severity;

    private Double cvssScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BugBountyReport.Status status;

    private long bountyAmountCents;

    @Column(length = 1000)
    private String resolutionNote;

    private Instant submittedAt;

    private Instant resolvedAt;
}
