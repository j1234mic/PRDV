package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.BiometricCredentialJpaRepository;
import com.prdv.rdv.iam.application.port.output.BiometricCredentialRepository;
import com.prdv.rdv.iam.domain.model.auth.BiometricCredential;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BiometricCredentialPersistenceAdapter implements BiometricCredentialRepository {

    private final BiometricCredentialJpaRepository jpa;
    private final AuthPersistenceMapper mapper;

    public BiometricCredentialPersistenceAdapter(BiometricCredentialJpaRepository jpa,
                                                 AuthPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public BiometricCredential save(BiometricCredential credential) {
        return mapper.toDomain(jpa.save(mapper.toEntity(credential)));
    }

    @Override
    public Optional<BiometricCredential> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BiometricCredential> findByCredentialId(String credentialId) {
        return jpa.findByCredentialId(credentialId).map(mapper::toDomain);
    }

    @Override
    public List<BiometricCredential> findByUserId(Long userId) {
        return jpa.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
