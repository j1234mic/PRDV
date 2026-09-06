package com.prdv.profile.domain.model;

import com.prdv.shared.exception.ValidationException;

/**
 * Value Object : numero RPPS (11 chiffres = 9 chiffres + cle de controle de 2 chiffres,
 * regle ANS : cle = 97 - (les 9 chiffres suivi de "00" modulo 97)).
 * Un numero invalide ne peut JAMAIS exister dans le domaine (construction = validation).
 */
public record RppsNumber(String value) {

    public RppsNumber {
        if (value == null || !value.matches("\\d{11}")) {
            throw new ValidationException("Numero RPPS invalide : 11 chiffres attendus");
        }
        if (expectedKey(value.substring(0, 9)) != Integer.parseInt(value.substring(9))) {
            throw new ValidationException("Numero RPPS invalide : cle de controle erronee");
        }
    }

    public static RppsNumber of(String raw) {
        return new RppsNumber(raw == null ? "" : raw.trim());
    }

    /** Utile pour creer un numero valide (seed, tests). */
    public static String completeWithKey(String nineDigits) {
        return nineDigits + String.format("%02d", expectedKey(nineDigits));
    }

    static int expectedKey(String nineDigits) {
        long base = Long.parseLong(nineDigits) * 100L;
        return (int) (97 - (base % 97));
    }
}
