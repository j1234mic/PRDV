package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.BiometricCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiometricCredentialJpaRepository extends JpaRepository<BiometricCredentialEntity, Long> {

    Optional<BiometricCredentialEntity> findByCredentialId(String credentialId);

    List<BiometricCredentialEntity> findByUserId(Long userId);
}
