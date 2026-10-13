package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.BugBountyReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BugBountyReportJpaRepository extends JpaRepository<BugBountyReportEntity, Long> {
}
