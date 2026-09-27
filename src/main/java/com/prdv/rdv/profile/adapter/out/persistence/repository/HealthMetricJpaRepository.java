package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.HealthMetricEntity;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Requêtes de mesures de sante.
 *
 * <p>Deux requetes distinctes (avec / sans type de mesure) plutot qu'un
 * {@code :type is null or ...} : la comparaison d'un parametre enumere nul
 * n'est pas portable d'un moteur SQL a l'autre.
 */
public interface HealthMetricJpaRepository extends JpaRepository<HealthMetricEntity, String> {

    @Query("""
            select m from HealthMetricEntity m
            where m.userId = :userId
              and m.metricType = :type
              and (:from is null or m.recordedAt >= :from)
              and (:to is null or m.recordedAt <= :to)
            order by m.recordedAt desc
            """)
    List<HealthMetricEntity> searchByType(@Param("userId") Long userId,
                                          @Param("type") HealthMetric.MetricType type,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to,
                                          Pageable pageable);

    @Query("""
            select m from HealthMetricEntity m
            where m.userId = :userId
              and (:from is null or m.recordedAt >= :from)
              and (:to is null or m.recordedAt <= :to)
            order by m.recordedAt desc
            """)
    List<HealthMetricEntity> searchAllTypes(@Param("userId") Long userId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to,
                                            Pageable pageable);

    void deleteByUserId(Long userId);
}
