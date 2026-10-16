package com.prdv.rdv.planning.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface PlanningAuditRepository extends JpaRepository<PlanningAuditEntity,UUID> {
    List<PlanningAuditEntity> findByPractitionerOrderByOccurredAtDescIdDesc(Long practitioner,Pageable page);
}
