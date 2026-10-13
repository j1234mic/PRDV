package com.prdv.rdv.iam.adapter.out.persistence.mapper;

import com.prdv.rdv.iam.adapter.out.persistence.entity.BugBountyReportEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.E2eeUserKeyEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.PentestReportEntity;
import com.prdv.rdv.iam.domain.model.security.BugBountyReport;
import com.prdv.rdv.iam.domain.model.security.E2eeUserKey;
import com.prdv.rdv.iam.domain.model.security.PentestReport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class SecurityPersistenceMapper {

    public E2eeUserKeyEntity toEntity(E2eeUserKey d) {
        E2eeUserKeyEntity e = new E2eeUserKeyEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setKeyId(d.getKeyId());
        e.setAlgorithm(d.getAlgorithm());
        e.setPublicKeyPem(d.getPublicKeyPem());
        e.setFingerprint(d.getFingerprint());
        e.setActive(d.isActive());
        e.setCreatedAt(d.getCreatedAt());
        e.setRotatedAt(d.getRotatedAt());
        return e;
    }

    public E2eeUserKey toDomain(E2eeUserKeyEntity e) {
        E2eeUserKey d = new E2eeUserKey();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setKeyId(e.getKeyId());
        d.setAlgorithm(e.getAlgorithm());
        d.setPublicKeyPem(e.getPublicKeyPem());
        d.setFingerprint(e.getFingerprint());
        d.setActive(e.isActive());
        d.setCreatedAt(e.getCreatedAt());
        d.setRotatedAt(e.getRotatedAt());
        return d;
    }

    public PentestReportEntity toEntity(PentestReport d) {
        PentestReportEntity e = new PentestReportEntity();
        e.setId(d.getId());
        e.setCampaignName(d.getCampaignName());
        e.setAuditor(d.getAuditor());
        e.setScope(d.getScope());
        e.setFindingsCount(d.getFindingsCount());
        e.setCriticalCount(d.getCriticalCount());
        e.setHighCount(d.getHighCount());
        e.setMediumCount(d.getMediumCount());
        e.setLowCount(d.getLowCount());
        e.setSecurityScore(d.getSecurityScore());
        e.setStatus(d.getStatus());
        e.setSummary(d.getSummary());
        e.setFindings(new ArrayList<>(d.getFindings()));
        e.setExecutedAt(d.getExecutedAt());
        e.setNextScheduledAt(d.getNextScheduledAt());
        return e;
    }

    public PentestReport toDomain(PentestReportEntity e) {
        PentestReport d = new PentestReport();
        d.setId(e.getId());
        d.setCampaignName(e.getCampaignName());
        d.setAuditor(e.getAuditor());
        d.setScope(e.getScope());
        d.setFindingsCount(e.getFindingsCount());
        d.setCriticalCount(e.getCriticalCount());
        d.setHighCount(e.getHighCount());
        d.setMediumCount(e.getMediumCount());
        d.setLowCount(e.getLowCount());
        d.setSecurityScore(e.getSecurityScore());
        d.setStatus(e.getStatus());
        d.setSummary(e.getSummary());
        d.setFindings(new ArrayList<>(e.getFindings()));
        d.setExecutedAt(e.getExecutedAt());
        d.setNextScheduledAt(e.getNextScheduledAt());
        return d;
    }

    public BugBountyReportEntity toEntity(BugBountyReport d) {
        BugBountyReportEntity e = new BugBountyReportEntity();
        e.setId(d.getId());
        e.setReporterEmail(d.getReporterEmail());
        e.setReporterAlias(d.getReporterAlias());
        e.setTitle(d.getTitle());
        e.setDescription(d.getDescription());
        e.setAffectedComponent(d.getAffectedComponent());
        e.setSeverity(d.getSeverity());
        e.setCvssScore(d.getCvssScore());
        e.setStatus(d.getStatus());
        e.setBountyAmountCents(d.getBountyAmountCents());
        e.setResolutionNote(d.getResolutionNote());
        e.setSubmittedAt(d.getSubmittedAt());
        e.setResolvedAt(d.getResolvedAt());
        return e;
    }

    public BugBountyReport toDomain(BugBountyReportEntity e) {
        BugBountyReport d = new BugBountyReport();
        d.setId(e.getId());
        d.setReporterEmail(e.getReporterEmail());
        d.setReporterAlias(e.getReporterAlias());
        d.setTitle(e.getTitle());
        d.setDescription(e.getDescription());
        d.setAffectedComponent(e.getAffectedComponent());
        d.setSeverity(e.getSeverity());
        d.setCvssScore(e.getCvssScore());
        d.setStatus(e.getStatus());
        d.setBountyAmountCents(e.getBountyAmountCents());
        d.setResolutionNote(e.getResolutionNote());
        d.setSubmittedAt(e.getSubmittedAt());
        d.setResolvedAt(e.getResolvedAt());
        return d;
    }
}
