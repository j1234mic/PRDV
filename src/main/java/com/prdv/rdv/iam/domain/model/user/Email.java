package com.prdv.rdv.iam.domain.model.user;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;

import java.util.regex.Pattern;

/**
 * Value Object : adresse email canonique (minuscule, sans espaces).
 * Le constructeur est prive : toute creation passe par {@link #of(String)}
 * qui garantit la validite (Design Pattern : Value Object).
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "L'email est obligatoire");
        }
        value = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Format d'email invalide : " + value);
        }
    }

    public static Email of(String raw) {
        return new Email(raw);
    }
}
