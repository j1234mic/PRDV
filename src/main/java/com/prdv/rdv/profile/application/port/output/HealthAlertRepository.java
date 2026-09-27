package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.health.HealthAlert;

import java.util.List;
import java.util.Optional;

/** Persistance des alertes automatiques de sante. */
public interface HealthAlertRepository {

    List<HealthAlert> saveAll(List<HealthAlert> alerts);

    Optional<HealthAlert> findById(String id);

    List<HealthAlert> findByUserId(Long userId, boolean onlyUnacknowledged);

    void deleteByUserId(Long userId);
}
