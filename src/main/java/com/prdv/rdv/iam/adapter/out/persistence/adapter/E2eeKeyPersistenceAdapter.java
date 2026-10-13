package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.mapper.SecurityPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.E2eeUserKeyJpaRepository;
import com.prdv.rdv.iam.application.port.output.E2eeKeyRepository;
import com.prdv.rdv.iam.domain.model.security.E2eeUserKey;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class E2eeKeyPersistenceAdapter implements E2eeKeyRepository {

    private final E2eeUserKeyJpaRepository jpa;
    private final SecurityPersistenceMapper mapper;

    public E2eeKeyPersistenceAdapter(E2eeUserKeyJpaRepository jpa,
                                     SecurityPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public E2eeUserKey save(E2eeUserKey key) {
        return mapper.toDomain(jpa.save(mapper.toEntity(key)));
    }

    @Override
    public Optional<E2eeUserKey> findActiveByUserId(Long userId) {
        return jpa.findFirstByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .map(mapper::toDomain);
    }

    @Override
    public List<E2eeUserKey> findByUserId(Long userId) {
        return jpa.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
