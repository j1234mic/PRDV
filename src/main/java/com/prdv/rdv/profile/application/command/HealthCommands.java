package com.prdv.rdv.profile.application.command;

import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Commandes du cas d'usage « suivi sante connectee » (module 2.1).
 */
public final class HealthCommands {

    private HealthCommands() {
    }

    public record ConnectDevice(ConnectedDevice.DeviceType type,
                                ConnectedDevice.Provider provider,
                                String externalDeviceId,
                                String label,
                                String model) {
    }

    /** Synchronisation d'un objet connecte sur une fenetre de temps. */
    public record SynchronizeDevice(String deviceId, LocalDate from, LocalDate to) {
    }

    public record IngestMetric(HealthMetric.MetricType type,
                               BigDecimal value,
                               HealthMetric.MeasurementContext context,
                               Instant recordedAt,
                               String deviceId,
                               String sourceLabel) {
    }

    public record IngestMetrics(String deviceId, List<IngestMetric> metrics) {
    }
}
