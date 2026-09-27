package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.HealthDtos;
import com.prdv.rdv.profile.application.port.input.ConnectedHealthUseCase;
import com.prdv.rdv.profile.application.port.input.HealthMetricsQueryUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Suivi sante connectee (module 2.1) : Apple Health / Google Fit / BLE,
 * montres, tensiometre, glucometre, balance, oxymetre, ECG portable,
 * series pour graphiques et alertes automatiques.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Sante connectee", description = "Objets connectes, mesures, series, alertes")
public class ConnectedHealthController {

    private static final int DEFAULT_LIMIT = 500;

    private final ConnectedHealthUseCase connectedHealthUseCase;
    private final HealthMetricsQueryUseCase metricsQueryUseCase;

    public ConnectedHealthController(ConnectedHealthUseCase connectedHealthUseCase,
                                     HealthMetricsQueryUseCase metricsQueryUseCase) {
        this.connectedHealthUseCase = connectedHealthUseCase;
        this.metricsQueryUseCase = metricsQueryUseCase;
    }

    @GetMapping("/devices")
    @PreAuthorize("hasAuthority('profile.health.read')")
    public List<ProfileViews.ConnectedDeviceView> myDevices() {
        return connectedHealthUseCase.myDevices();
    }

    @PostMapping("/devices")
    @PreAuthorize("hasAuthority('profile.health.write')")
    public ProfileViews.ConnectedDeviceView connectDevice(
            @Valid @RequestBody HealthDtos.ConnectDeviceRequest request) {
        return connectedHealthUseCase.connectDevice(request.toCommand());
    }

    @DeleteMapping("/devices/{deviceId}")
    @PreAuthorize("hasAuthority('profile.health.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnectDevice(@PathVariable String deviceId) {
        connectedHealthUseCase.disconnectDevice(deviceId);
    }

    @PostMapping("/devices/{deviceId}/sync")
    @PreAuthorize("hasAuthority('profile.health.write')")
    @Operation(summary = "Synchronisation avec la plateforme source")
    public ProfileViews.SyncReportView synchronize(
            @PathVariable String deviceId,
            @Valid @RequestBody HealthDtos.SyncRequest request) {
        return connectedHealthUseCase.synchronize(request.toCommand(deviceId));
    }

    @PostMapping("/metrics")
    @PreAuthorize("hasAuthority('profile.health.write')")
    @Operation(summary = "Import par lot : les alertes sont evaluees a l'ingestion")
    public ProfileViews.SyncReportView ingest(@Valid @RequestBody HealthDtos.MetricsRequest request) {
        return connectedHealthUseCase.ingest(request.toCommand());
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('profile.health.read')")
    public List<ProfileViews.HealthMetricView> metrics(
            @RequestParam(required = false) HealthMetric.MetricType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return metricsQueryUseCase.metrics(null, type, from, to, limit);
    }

    @GetMapping("/series")
    @PreAuthorize("hasAuthority('profile.health.read')")
    @Operation(summary = "Serie agregee (HOUR, DAY, WEEK) prete a tracer")
    public ProfileViews.MetricSeriesView series(
            @RequestParam HealthMetric.MetricType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "DAY") String bucket) {
        return metricsQueryUseCase.series(null, type, from, to, bucket);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('profile.health.read')")
    public List<ProfileViews.HealthAlertView> alerts(
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        return metricsQueryUseCase.alerts(null, onlyActive);
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("hasAuthority('profile.health.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledgeAlert(@PathVariable String alertId) {
        metricsQueryUseCase.acknowledgeAlert(alertId);
    }
}
