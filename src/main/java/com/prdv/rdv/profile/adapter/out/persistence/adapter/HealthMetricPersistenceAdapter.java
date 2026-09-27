package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.mapper.HealthPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.HealthMetricJpaRepository;
import com.prdv.rdv.profile.application.port.output.HealthMetricRepository;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Adapteur JPA des mesures de sante.
 *
 * <p>Les mesures sont inserees par lot et lues par fenetre de temps bornee :
 * la volumetrie est maitrisee par la pagination appliquee cote requete.
 */
@Repository
public class HealthMetricPersistenceAdapter implements HealthMetricRepository {

    private final HealthMetricJpaRepository jpa;
    private final HealthPersistenceMapper mapper;

    public HealthMetricPersistenceAdapter(HealthMetricJpaRepository jpa, HealthPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public List<HealthMetric> saveAll(List<HealthMetric> metrics) {
        return jpa.saveAll(metrics.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<HealthMetric> find(Long userId, HealthMetric.MetricType type, Instant from, Instant to,
                                   int limit) {
        PageRequest page = PageRequest.of(0, Math.max(limit, 1));
        var entities = type == null
                ? jpa.searchAllTypes(userId, from, to, page)
                : jpa.searchByType(userId, type, from, to, page);
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }
}
