package com.prdv.rdv.profile.domain.model.common;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Formats des coordonnees saisies (telephone, email, URL).
 *
 * <p>Controles partages par le domaine patient et le domaine praticien : une
 * saisie invalide est refusee de la meme maniere partout, et les valeurs
 * vides sont normalisees en {@code null} (« non renseigne »).
 */
public final class ContactFormats {

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9\\s().-]{5,24}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern HTTP_URL = Pattern.compile("^https?://[^\\s/?#]+(?:[/?#]\\S*)?$",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_URL_CHARS = 500;

    private ContactFormats() {
    }

    /** Numero de telephone ou de fax (chiffres, espaces, points, tirets, indicatif). */
    public static String phone(String raw, ProfileErrorCode code, String label) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        if (!PHONE.matcher(value).matches()) {
            throw invalid(code, label, value);
        }
        return value;
    }

    /** Adresse email, normalisee en minuscules. */
    public static String email(String raw, ProfileErrorCode code, String label) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw invalid(code, label, value);
        }
        return normalized;
    }

    /** URL absolue http(s), bornee en longueur. */
    public static String httpUrl(String raw, ProfileErrorCode code, String label) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        if (value.length() > MAX_URL_CHARS || !HTTP_URL.matcher(value).matches()) {
            throw invalid(code, label, value);
        }
        return value;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ProfileException invalid(ProfileErrorCode code, String label, String value) {
        return ProfileException.of(code, label + " invalide : " + value);
    }
}
