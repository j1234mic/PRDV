package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.BiometricCredential;

import java.util.List;
import java.util.Optional;

public interface BiometricCredentialRepository {

    BiometricCredential save(BiometricCredential credential);

    Optional<BiometricCredential> findById(Long id);

    Optional<BiometricCredential> findByCredentialId(String credentialId);

    List<BiometricCredential> findByUserId(Long userId);
}
