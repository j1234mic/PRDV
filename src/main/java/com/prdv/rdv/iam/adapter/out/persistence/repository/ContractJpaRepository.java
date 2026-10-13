package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.PractitionerContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractJpaRepository extends JpaRepository<PractitionerContractEntity, Long> {

    Optional<PractitionerContractEntity> findTopByPractitionerUserIdOrderByAcceptedAtDesc(Long practitionerUserId);

    List<PractitionerContractEntity> findByPractitionerUserIdOrderByAcceptedAtDesc(Long practitionerUserId);

    boolean existsByPractitionerUserId(Long practitionerUserId);
}
