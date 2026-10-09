package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.HealthCommands;
import com.prdv.rdv.profile.application.port.input.ConnectedHealthUseCase;
import com.prdv.rdv.profile.application.port.output.ConnectedDeviceRepository;
import com.prdv.rdv.profile.application.port.output.ConnectedHealthProviderPort;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAccessGuard;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.HealthMetricRecorder;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.config.ProfileProperties;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Cas d'usage : suivi sante connectee (module 2.1).
 *
 * <p>L'appairage d'un objet connecte vaut consentement explicite a la
 * collecte (finalite {@code CONNECTED_DEVICES}) : la preuve est enregistree
 * dans les preferences avant toute importation de mesures.
 *
 * <p>La plateforme source est resolue par strategie
 * ({@link ConnectedHealthProviderPort}) : Apple Health, Google Fit ou
 * Bluetooth, sans que ce service n'en connaisse le protocole.
 */
@Service
public class ConnectedHealthService implements ConnectedHealthUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConnectedHealthService.class);

    private final ConnectedDeviceRepository deviceRepository;
    private final List<ConnectedHealthProviderPort> providers;
    private final HealthMetricRecorder metricRecorder;
    private final CurrentUserPort currentUser;
    private final ProfileAccessGuard accessGuard;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final ProfileProperties properties;
    private final Clock clock;

    public ConnectedHealthService(ConnectedDeviceRepository deviceRepository,
                                  List<ConnectedHealthProviderPort> providers,
                                  HealthMetricRecorder metricRecorder,
                                  CurrentUserPort currentUser,
                                  ProfileAccessGuard accessGuard,
                                  ProfileViewMapper viewMapper,
                                  ProfileAuditTrail auditTrail,
                                  ProfileEventPublisher eventPublisher,
                                  ProfileProperties properties,
                                  Clock clock) {
        this.deviceRepository = deviceRepository;
        this.providers = providers;
        this.metricRecorder = metricRecorder;
        this.currentUser = currentUser;
        this.accessGuard = accessGuard;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.ConnectedDeviceView> myDevices() {
        Long userId = currentUser.requireCurrentUserId();
        return deviceRepository.findByUserId(userId).stream().map(viewMapper::deviceView).toList();
    }

    @Override
    @Transactional
    public ProfileViews.ConnectedDeviceView connectDevice(HealthCommands.ConnectDevice command) {
        Long userId = currentUser.requireCurrentUserId();
        if (command.externalDeviceId() != null && !command.externalDeviceId().isBlank()) {
            deviceRepository.findByUserIdAndExternalId(userId, command.externalDeviceId())
                    .filter(ConnectedDevice::isConnected)
                    .ifPresent(existing -> {
                        throw ProfileException.of(ProfileErrorCode.DEVICE_ALREADY_CONNECTED,
                                "Cet objet est deja appaire (" + existing.getLabel() + ")");
                    });
        }

        ConnectedDevice device = ConnectedDevice.connect(userId, command.type(), command.provider(),
                command.externalDeviceId(), command.label(), command.model(), clock);
        ConnectedDevice saved = deviceRepository.save(device);

        PrivacyPreferences preferences = accessGuard.preferencesOf(userId);
        if (!preferences.hasConsent(PrivacyPreferences.ConsentPurpose.CONNECTED_DEVICES)) {
            preferences.recordConsent(PrivacyPreferences.ConsentPurpose.CONNECTED_DEVICES, true,
                    "device-pairing", null, clock);
            auditTrail.success(ProfileAuditPort.ProfileAuditAction.CONSENT_RECORDED, userId,
                    "consentement collecte des donnees d'objets connectes");
        }

        auditTrail.success(ProfileAuditPort.ProfileAuditAction.DEVICE_CONNECTED, userId,
                "ConnectedDevice", saved.getId(), saved.getType() + " via " + saved.getProvider());
        return viewMapper.deviceView(saved);
    }

    @Override
    @Transactional
    public void disconnectDevice(String deviceId) {
        Long userId = currentUser.requireCurrentUserId();
        ConnectedDevice device = requireOwnedDevice(deviceId, userId);
        device.disconnect(clock);
        deviceRepository.save(device);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.DEVICE_CONNECTED, userId,
                "ConnectedDevice", deviceId, "objet connecte retire");
    }

    @Override
    @Transactional
    public ProfileViews.SyncReportView synchronize(HealthCommands.SynchronizeDevice command) {
        Long userId = currentUser.requireCurrentUserId();
        ConnectedDevice device = requireOwnedDevice(command.deviceId(), userId);

        Instant from = command.from() == null
                ? LocalDate.now(clock).minusDays(properties.getHealth().getDefaultSyncWindowDays())
                .atStartOfDay(ZoneOffset.UTC).toInstant()
                : command.from().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = command.to() == null ? clock.instant()
                : command.to().atStartOfDay(ZoneOffset.UTC).toInstant();

        ConnectedHealthProviderPort provider = providerFor(device);
        List<HealthMetric> fetched = provider.fetch(userId, device, from, to);
        SyncOutcome outcome = persist(userId, device.getId(), fetched, provider.name());

        device.markSynchronized(outcome.accepted().size(), clock);
        deviceRepository.save(device);
        eventPublisher.publish(new ProfileEvent.ConnectedDeviceSynchronized(userId, device.getId(),
                device.getType().name(), outcome.accepted().size(), clock.instant()));
        return reportView(device, outcome);
    }

    @Override
    @Transactional
    public ProfileViews.SyncReportView ingest(HealthCommands.IngestMetrics command) {
        Long userId = currentUser.requireCurrentUserId();
        if (command.metrics() == null || command.metrics().isEmpty()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Aucune mesure transmise");
        }
        if (command.metrics().size() > properties.getHealth().getMaxMetricsPerBatch()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Import limite a " + properties.getHealth().getMaxMetricsPerBatch() + " mesures");
        }

        List<HealthMetric> metrics = new ArrayList<>();
        int rejected = 0;
        for (HealthCommands.IngestMetric entry : command.metrics()) {
            try {
                metrics.add(HealthMetric.of(userId, entry.deviceId() == null
                        ? command.deviceId() : entry.deviceId(), entry.type(), entry.value(),
                        entry.context(), entry.recordedAt() == null ? clock.instant() : entry.recordedAt(),
                        entry.sourceLabel()));
            } catch (ProfileException e) {
                rejected++;
                log.debug("Mesure rejetee ({} {}) : {}", entry.type(), entry.value(), e.getMessage());
            }
        }
        SyncOutcome outcome = persist(userId, command.deviceId(), metrics, "MANUAL");

        if (command.deviceId() != null) {
            deviceRepository.findById(command.deviceId())
                    .filter(device -> device.getUserId().equals(userId) && device.isConnected())
                    .ifPresent(device -> {
                        device.markSynchronized(outcome.accepted().size(), clock);
                        deviceRepository.save(device);
                    });
        }
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.DEVICE_CONNECTED, userId,
                "HealthMetric", command.deviceId(),
                outcome.accepted().size() + " mesures importees, " + (rejected + outcome.rejected())
                        + " rejetees");
        return new ProfileViews.SyncReportView(command.deviceId(), outcome.accepted().size(),
                rejected + outcome.rejected(),
                outcome.alerts().stream().map(viewMapper::alertView).toList());
    }

    // ------------------------------------------------------------------

    /** Persiste les mesures, evalue les alertes et notifie le patient (voir {@link HealthMetricRecorder}). */
    private SyncOutcome persist(Long userId, String deviceId, List<HealthMetric> metrics, String source) {
        HealthMetricRecorder.Outcome outcome = metricRecorder.record(userId, deviceId, metrics, source);
        return new SyncOutcome(outcome.accepted(), outcome.alerts(), 0);
    }

    private ConnectedHealthProviderPort providerFor(ConnectedDevice device) {
        return providers.stream()
                .filter(provider -> provider.supports(device.getProvider()))
                .findFirst()
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                        "Aucun adapteur disponible pour la plateforme " + device.getProvider()));
    }

    private ConnectedDevice requireOwnedDevice(String deviceId, Long userId) {
        ConnectedDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.DEVICE_NOT_FOUND,
                        "Objet connecte " + deviceId + " introuvable"));
        if (!device.getUserId().equals(userId)) {
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Cet objet connecte appartient a un autre utilisateur");
        }
        return device;
    }

    private ProfileViews.SyncReportView reportView(ConnectedDevice device, SyncOutcome outcome) {
        return new ProfileViews.SyncReportView(device.getId(), outcome.accepted().size(),
                outcome.rejected(), outcome.alerts().stream().map(viewMapper::alertView).toList());
    }

    /** Resultat interne d'une importation. */
    private record SyncOutcome(List<HealthMetric> accepted, List<HealthAlert> alerts, int rejected) {
    }

    /** Duree de la fenetre de synchronisation (utilisee par les tests et l'audit). */
    Duration windowOf(LocalDate from, LocalDate to) {
        return Duration.between(from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
