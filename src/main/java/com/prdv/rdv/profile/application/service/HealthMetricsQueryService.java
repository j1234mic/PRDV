package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.port.input.HealthMetricsQueryUseCase;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.HealthAlertRepository;
import com.prdv.rdv.profile.application.port.output.HealthMetricRepository;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAccessGuard;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.config.ProfileProperties;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cas d'usage : consultation des mesures de sante, series temporelles
 * (graphiques d'evolution) et alertes.
 *
 * <p>L'agregation (heure / jour / semaine) est calculee ici plutot qu'en
 * base : elle reste ainsi testable et independante du moteur SQL, et peut
 * etre deleguee plus tard a une base de series temporelles sans changer le
 * contrat du port.
 */
@Service
public class HealthMetricsQueryService implements HealthMetricsQueryUseCase {

    public static final String BUCKET_HOUR = "HOUR";
    public static final String BUCKET_DAY = "DAY";
    public static final String BUCKET_WEEK = "WEEK";

    private final HealthMetricRepository metricRepository;
    private final HealthAlertRepository alertRepository;
    private final CurrentUserPort currentUser;
    private final ProfileAccessGuard accessGuard;
    private final ProfileViewMapper viewMapper;
    private final ProfileProperties properties;
    private final Clock clock;

    public HealthMetricsQueryService(HealthMetricRepository metricRepository,
                                     HealthAlertRepository alertRepository,
                                     CurrentUserPort currentUser,
                                     ProfileAccessGuard accessGuard,
                                     ProfileViewMapper viewMapper,
                                     ProfileProperties properties,
                                     Clock clock) {
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.currentUser = currentUser;
        this.accessGuard = accessGuard;
        this.viewMapper = viewMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.HealthMetricView> metrics(Long patientUserId, HealthMetric.MetricType type,
                                                       Instant from, Instant to, int limit) {
        Long target = accessGuard.resolveTarget(patientUserId);
        accessGuard.requireAccess(target, PrivacyPreferences.DataCategory.CONNECTED_HEALTH);
        int max = Math.min(Math.max(limit, 1), properties.getHealth().getMaxMetricsPerQuery());
        return metricRepository.find(target, type, from, to, max).stream()
                .map(viewMapper::metricView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.MetricSeriesView series(Long patientUserId, HealthMetric.MetricType type,
                                                Instant from, Instant to, String bucket) {
        Long target = accessGuard.resolveTarget(patientUserId);
        accessGuard.requireAccess(target, PrivacyPreferences.DataCategory.CONNECTED_HEALTH);
        if (type == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le type de mesure est obligatoire pour tracer une serie");
        }
        String normalizedBucket = normalizeBucket(bucket);
        List<HealthMetric> metrics = metricRepository.find(target, type, from, to,
                properties.getHealth().getMaxMetricsPerQuery());

        Map<Instant, List<BigDecimal>> grouped = new LinkedHashMap<>();
        for (HealthMetric metric : metrics) {
            grouped.computeIfAbsent(bucketStart(metric.recordedAt(), normalizedBucket),
                    key -> new ArrayList<>()).add(metric.value());
        }
        List<ProfileViews.MetricPointView> points = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> point(entry.getKey(), entry.getValue()))
                .toList();

        return new ProfileViews.MetricSeriesView(type, type.unit(), normalizedBucket, points,
                stats(points));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.HealthAlertView> alerts(Long patientUserId, boolean onlyActive) {
        Long target = accessGuard.resolveTarget(patientUserId);
        accessGuard.requireAccess(target, PrivacyPreferences.DataCategory.CONNECTED_HEALTH);
        return alertRepository.findByUserId(target, onlyActive).stream()
                .map(viewMapper::alertView)
                .toList();
    }

    @Override
    @Transactional
    public void acknowledgeAlert(String alertId) {
        Long userId = currentUser.requireCurrentUserId();
        HealthAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.ENTRY_NOT_FOUND,
                        "Alerte " + alertId + " introuvable"));
        if (!alert.getUserId().equals(userId)) {
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Cette alerte appartient a un autre utilisateur");
        }
        alert.acknowledge(clock);
        alertRepository.saveAll(List.of(alert));
    }

    // ------------------------------------------------------------------

    private ProfileViews.MetricPointView point(Instant bucketStart, List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        BigDecimal min = values.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal max = values.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        return new ProfileViews.MetricPointView(bucketStart, average, min, max, values.size());
    }

    private ProfileViews.MetricStatsView stats(List<ProfileViews.MetricPointView> points) {
        if (points.isEmpty()) {
            return new ProfileViews.MetricStatsView(null, null, null, 0);
        }
        int count = points.stream().mapToInt(ProfileViews.MetricPointView::count).sum();
        BigDecimal min = points.stream().map(ProfileViews.MetricPointView::min)
                .min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = points.stream().map(ProfileViews.MetricPointView::max)
                .max(Comparator.naturalOrder()).orElse(null);
        BigDecimal average = points.stream().map(ProfileViews.MetricPointView::average)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
        return new ProfileViews.MetricStatsView(min, max, average, count);
    }

    private Instant bucketStart(Instant instant, String bucket) {
        return switch (bucket) {
            case BUCKET_HOUR -> instant.truncatedTo(ChronoUnit.HOURS);
            case BUCKET_WEEK -> LocalDate.ofInstant(instant, ZoneOffset.UTC)
                    .with(DayOfWeek.MONDAY)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();
            default -> instant.truncatedTo(ChronoUnit.DAYS);
        };
    }

    private String normalizeBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            return BUCKET_DAY;
        }
        String normalized = bucket.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.equals(BUCKET_HOUR) && !normalized.equals(BUCKET_DAY)
                && !normalized.equals(BUCKET_WEEK)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Aggregation inconnue : " + bucket + " (valeurs acceptees : HOUR, DAY, WEEK)");
        }
        return normalized;
    }
}
