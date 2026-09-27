package com.prdv.rdv.profile.adapter.in.web.dto;

import com.prdv.rdv.profile.application.command.HealthCommands;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** DTO d'entree de la sante connectee. */
public final class HealthDtos {

    private HealthDtos() {
    }

    public record ConnectDeviceRequest(@NotNull ConnectedDevice.DeviceType type,
                                       @NotNull ConnectedDevice.Provider provider,
                                       @NotBlank @Size(max = 150) String externalDeviceId,
                                       @Size(max = 150) String label,
                                       @Size(max = 100) String model) {

        public HealthCommands.ConnectDevice toCommand() {
            return new HealthCommands.ConnectDevice(type, provider, externalDeviceId, label, model);
        }
    }

    public record SyncRequest(LocalDate from, LocalDate to) {

        public HealthCommands.SynchronizeDevice toCommand(String deviceId) {
            return new HealthCommands.SynchronizeDevice(deviceId, from, to);
        }
    }

    public record MetricRequest(@NotNull HealthMetric.MetricType type,
                                @NotNull BigDecimal value,
                                HealthMetric.MeasurementContext context,
                                @NotNull Instant recordedAt,
                                String sourceLabel) {

        public HealthCommands.IngestMetric toCommand(String deviceId) {
            return new HealthCommands.IngestMetric(type, value, context, recordedAt, deviceId, sourceLabel);
        }
    }

    public record MetricsRequest(@NotBlank String deviceId,
                                 @NotEmpty List<@Valid MetricRequest> metrics) {

        public HealthCommands.IngestMetrics toCommand() {
            return new HealthCommands.IngestMetrics(deviceId,
                    metrics.stream().map(metric -> metric.toCommand(deviceId)).toList());
        }
    }
}
