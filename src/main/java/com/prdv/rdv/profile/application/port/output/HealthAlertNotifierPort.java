package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.health.HealthAlert;

import java.util.List;

/**
 * Port de sortie : notification des alertes de sante (push, SMS, email).
 * L'adapteur fourni journalise ; un adapteur SMS/push peut s'y substituer.
 */
public interface HealthAlertNotifierPort {

    void notify(Long userId, List<HealthAlert> alerts);
}
