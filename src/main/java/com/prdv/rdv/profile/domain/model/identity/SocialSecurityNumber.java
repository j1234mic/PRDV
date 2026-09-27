package com.prdv.rdv.profile.domain.model.identity;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.util.regex.Pattern;

/**
 * Value Object : numero de securite sociale francais (NIR).
 *
 * <p>15 caracteres : 13 chiffres significatifs + cle de controle de 2 chiffres.
 * La cle vaut {@code 97 - (NIR13 modulo 97)}. Les numeros corses utilisent
 * {@code 2A} / {@code 2B} : ils sont respectivement ramenes a {@code 19} et
 * {@code 18} avant le calcul de la cle (regle officielle GIP-MDS).
 *
 * <p>Le numero brut n'est jamais conserve en base : seul un jeton
 * (tokenisation HMAC) et une valeur masquee le sont
 * (cf. {@code SensitiveDataProtector}).
 */
public record SocialSecurityNumber(String value) {

    /** Sexe (1) + annee (2) + mois (2) + departement (2, dont 2A/2B) + commune/ordre (6) + cle (2). */
    private static final Pattern NIR_PATTERN = Pattern.compile("^[12][0-9]{4}(?:[0-9]{2}|2[AB])[0-9]{6}[0-9]{2}$");

    public SocialSecurityNumber {
        if (value == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le numero de securite sociale est obligatoire");
        }
        value = value.replaceAll("[\\s.-]", "").toUpperCase(java.util.Locale.ROOT);
        if (!NIR_PATTERN.matcher(value).matches()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Format de numero de securite sociale invalide (15 caracteres attendus) : " + value);
        }
        if (controlKey(value) != declaredKey(value)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Cle de controle du numero de securite sociale invalide");
        }
    }

    public static SocialSecurityNumber of(String raw) {
        return new SocialSecurityNumber(raw);
    }

    /** Cle attendue selon l'algorithme officiel (97 - NIR13 mod 97). */
    public static int controlKey(String nir15) {
        String numeric = nir15.substring(0, 13).replace("2A", "19").replace("2B", "18");
        long base = Long.parseLong(numeric);
        return (int) (97 - (base % 97));
    }

    private static int declaredKey(String nir15) {
        return Integer.parseInt(nir15.substring(13));
    }

    /** Valeur affichable : seuls le sexe, l'annee, le mois et la cle restent lisibles. */
    public String masked() {
        return value.substring(0, 7) + "****" + value.substring(13);
    }
}
