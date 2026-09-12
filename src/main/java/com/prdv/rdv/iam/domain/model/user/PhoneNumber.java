package com.prdv.rdv.iam.domain.model.user;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;

import java.util.regex.Pattern;

/**
 * Value Object : numero de telephone au format international E.164
 * (ex : +33612345678). Utilise pour l'OTP SMS.
 */
public record PhoneNumber(String value) {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Le numero de telephone est obligatoire");
        }
        value = value.replaceAll("\\s+", "");
        if (!E164_PATTERN.matcher(value).matches()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le numero doit etre au format E.164 (ex : +33612345678) : " + value);
        }
    }

    public static PhoneNumber of(String raw) {
        return new PhoneNumber(raw);
    }
}
