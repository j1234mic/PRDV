package com.prdv.rdv.profile.domain.model.practitioner;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.util.regex.Pattern;

/**
 * Value Object : numero SIRET (14 chiffres, Systeme d'Identification du
 * Repertoire des ETablissements).
 *
 * <p>La validite est verifiee par l'algorithme de Luhn : une faute de frappe
 * est refusee immediatement au lieu de produire un dossier professionnel
 * incoherent.
 */
public record Siret(String value) {

    private static final Pattern SIRET_PATTERN = Pattern.compile("^[0-9]{14}$");

    public Siret {
        if (value == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le numero SIRET est obligatoire");
        }
        value = value.replaceAll("[\\s.-]", "");
        if (!SIRET_PATTERN.matcher(value).matches()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le SIRET doit comporter 14 chiffres : " + value);
        }
        if (!passesLuhn(value)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La cle de controle du SIRET est invalide");
        }
    }

    public static Siret of(String raw) {
        return new Siret(raw);
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int digit = digits.charAt(digits.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }
}
