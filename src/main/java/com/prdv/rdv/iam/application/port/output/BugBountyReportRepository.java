package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.security.BugBountyReport;

import java.util.List;
import java.util.Optional;

public interface BugBountyReportRepository {

    BugBountyReport save(BugBountyReport report);

    Optional<BugBountyReport> findById(Long id);

    List<BugBountyReport> findAll();
}
