package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.UserSession;

/**
 * Port de detection de fraude. L'implementation fournie est un moteur de
 * regles (chaine de Specifications : nouvel appareil, nouveau pays,
 * « impossible travel »). En production, un adapteur IA / scoring tiers
 * se branche sans modifier le domaine.
 */
public interface FraudDetectionPort {

    FraudAssessment assess(FraudContext context);

    enum RiskLevel { LOW, MEDIUM, HIGH }

    record FraudContext(Long userId,
                        String ipAddress,
                        UserSession.GeoLocation currentGeo,
                        UserSession previousSession) {
    }

    record FraudAssessment(RiskLevel riskLevel, java.util.List<String> reasons, boolean stepUpRequired) {

        public static FraudAssessment low() {
            return new FraudAssessment(RiskLevel.LOW, java.util.List.of(), false);
        }
    }
}
