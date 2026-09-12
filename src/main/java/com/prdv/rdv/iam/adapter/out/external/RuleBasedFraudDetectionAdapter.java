package com.prdv.rdv.iam.adapter.out.external;

import com.prdv.rdv.iam.application.port.output.FraudDetectionPort;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Moteur de detection de fraude par regles (pattern Chain of Specifications) :
 * <ol>
 *   <li>nouveau pays par rapport a la derniere session ;</li>
 *   <li>« impossible travel » : deux ouvertures distantes de plus de 1000 km
 *       en moins de 3 heures.</li>
 * </ol>
 * Un adapteur de scoring IA se substituera a cette classe via le port.
 */
@Component
public class RuleBasedFraudDetectionAdapter implements FraudDetectionPort {

    private static final double IMPOSSIBLE_SPEED_KMH = 1000.0;
    private static final long IMPOSSIBLE_WINDOW_HOURS = 3;

    @Override
    public FraudAssessment assess(FraudContext context) {
        UserSession previous = context.previousSession();
        UserSession.GeoLocation current = context.currentGeo();

        if (previous == null || current == null
                || !current.isKnown() || previous.getGeoLocation() == null
                || !previous.getGeoLocation().isKnown()) {
            return FraudAssessment.low();
        }

        List<String> reasons = new ArrayList<>();
        UserSession.GeoLocation previousGeo = previous.getGeoLocation();

        boolean newCountry = current.country() != null
                && !current.country().equalsIgnoreCase(previousGeo.country());
        if (newCountry) {
            reasons.add("NOUVEAU_PAYS(" + previousGeo.country() + "->" + current.country() + ")");
        }

        if (current.latitude() != null && current.longitude() != null
                && previousGeo.latitude() != null && previousGeo.longitude() != null) {
            double distanceKm = haversine(previousGeo.latitude(), previousGeo.longitude(),
                    current.latitude(), current.longitude());
            long hours = Math.max(1, Duration.between(previous.getLastSeenAt(), Instant.now()).toHours());
            double speed = distanceKm / hours;
            if (hours < IMPOSSIBLE_WINDOW_HOURS && speed > IMPOSSIBLE_SPEED_KMH) {
                reasons.add("DEPLACEMENT_IMPOSSIBLE(" + Math.round(distanceKm) + "km en " + hours + "h)");
            }
        }

        if (reasons.isEmpty()) {
            return FraudAssessment.low();
        }
        return new FraudAssessment(RiskLevel.HIGH, reasons, true);
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
