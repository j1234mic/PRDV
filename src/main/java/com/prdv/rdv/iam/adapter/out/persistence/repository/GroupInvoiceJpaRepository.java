package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.GroupInvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupInvoiceJpaRepository extends JpaRepository<GroupInvoiceEntity, Long> {

    List<GroupInvoiceEntity> findByEstablishmentUserIdOrderByCreatedAtDesc(Long establishmentUserId);
}
