package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.mapper.HealthPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.HealthAlertJpaRepository;
import com.prdv.rdv.profile.application.port.output.HealthAlertRepository;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapteur JPA des alertes automatiques de sante. */
@Repository
public class HealthAlertPersistenceAdapter implements HealthAlertRepository {

    private final HealthAlertJpaRepository jpa;
    private final HealthPersistenceMapper mapper;

    public HealthAlertPersistenceAdapter(HealthAlertJpaRepository jpa, HealthPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public List<HealthAlert> saveAll(List<HealthAlert> alerts) {
        return jpa.saveAll(alerts.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<HealthAlert> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<HealthAlert> findByUserId(Long userId, boolean onlyUnacknowledged) {
        var entities = onlyUnacknowledged
                ? jpa.findByUserIdAndAcknowledgedFalseOrderByTriggeredAtDesc(userId)
                : jpa.findByUserIdOrderByTriggeredAtDesc(userId);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }
}
