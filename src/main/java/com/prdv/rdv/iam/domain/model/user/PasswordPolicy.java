package com.prdv.rdv.iam.domain.model.user;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;

/**
 * Regle metier du domaine : politique de robustesse des mots de passe
 * (Specification reutilisable, sans dependance technique).
 */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;

    private PasswordPolicy() {
    }

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
            throw IamException.of(IamErrorCode.PASSWORD_TOO_WEAK,
                    "Le mot de passe doit contenir au moins " + MIN_LENGTH + " caracteres");
        }
        if (!rawPassword.matches(".*[a-z].*")) {
            throw IamException.of(IamErrorCode.PASSWORD_TOO_WEAK,
                    "Le mot de passe doit contenir au moins une minuscule");
        }
        if (!rawPassword.matches(".*[A-Z].*")) {
            throw IamException.of(IamErrorCode.PASSWORD_TOO_WEAK,
                    "Le mot de passe doit contenir au moins une majuscule");
        }
        if (!rawPassword.matches(".*\\d.*")) {
            throw IamException.of(IamErrorCode.PASSWORD_TOO_WEAK,
                    "Le mot de passe doit contenir au moins un chiffre");
        }
    }
}
