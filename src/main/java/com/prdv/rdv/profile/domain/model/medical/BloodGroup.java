package com.prdv.rdv.profile.domain.model.medical;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.util.Locale;

/**
 * Value Object : groupe sanguin (systeme ABO + facteur Rhesus).
 * Accepte les ecritures usuelles : « A+ », « o neg », « AB- »...
 */
public record BloodGroup(AboGroup abo, RhFactor rh) {

    public enum AboGroup { A, B, AB, O }

    public enum RhFactor { POSITIVE, NEGATIVE }

    public BloodGroup {
        if (abo == null || rh == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le groupe sanguin exige un systeme ABO et un facteur Rhesus");
        }
    }

    public static BloodGroup of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le groupe sanguin est obligatoire");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        RhFactor rh;
        if (normalized.endsWith("+") || normalized.endsWith("POS") || normalized.endsWith("POSITIF")) {
            rh = RhFactor.POSITIVE;
        } else if (normalized.endsWith("-") || normalized.endsWith("NEG") || normalized.endsWith("NEGATIF")) {
            rh = RhFactor.NEGATIVE;
        } else {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Facteur Rhesus manquant (ex : A+, O-) : " + raw);
        }
        String aboPart = normalized.replaceAll("[+\\-].*$", "").replace("POS", "").replace("NEG", "");
        try {
            return new BloodGroup(AboGroup.valueOf(aboPart), rh);
        } catch (IllegalArgumentException e) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Systeme ABO inconnu (A, B, AB, O) : " + raw);
        }
    }

    @Override
    public String toString() {
        return abo.name() + (rh == RhFactor.POSITIVE ? "+" : "-");
    }
}
