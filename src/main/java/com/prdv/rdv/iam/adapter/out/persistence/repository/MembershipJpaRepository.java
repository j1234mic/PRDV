package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.EstablishmentMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipJpaRepository extends JpaRepository<EstablishmentMembershipEntity, Long> {

    List<EstablishmentMembershipEntity> findByEstablishmentUserId(Long establishmentUserId);

    List<EstablishmentMembershipEntity> findByPractitionerUserId(Long practitionerUserId);
}
