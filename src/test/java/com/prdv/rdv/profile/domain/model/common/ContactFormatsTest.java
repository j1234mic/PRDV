package com.prdv.rdv.profile.domain.model.common;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Formats de coordonnees partages par le domaine patient et le domaine praticien. */
class ContactFormatsTest {

    @Test
    @DisplayName("Un telephone valide est conserve (espaces de bord retires), une valeur vide reste non renseignee")
    void phoneAcceptsCommonFormatsAndTreatsBlankAsUnset() {
        assertThat(ContactFormats.phone(" +33 6 12 34 56 78 ", ProfileErrorCode.VALIDATION_ERROR, "Telephone"))
                .isEqualTo("+33 6 12 34 56 78");
        assertThat(ContactFormats.phone("01.23.45.67.89", ProfileErrorCode.VALIDATION_ERROR, "Telephone"))
                .isEqualTo("01.23.45.67.89");
        assertThat(ContactFormats.phone("   ", ProfileErrorCode.VALIDATION_ERROR, "Telephone")).isNull();
        assertThat(ContactFormats.phone(null, ProfileErrorCode.VALIDATION_ERROR, "Telephone")).isNull();
    }

    @Test
    @DisplayName("Un telephone invalide est refuse avec le libelle du champ")
    void phoneRejectsInvalidValues() {
        assertThatThrownBy(() -> ContactFormats.phone("abc", ProfileErrorCode.LOCATION_INVALID, "Telephone fixe"))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Telephone fixe invalide");
    }

    @Test
    @DisplayName("Un email est normalise en minuscules ; un email malforme est refuse")
    void emailIsNormalisedAndMalformedEmailRejected() {
        assertThat(ContactFormats.email("  Dr.Martin@Cabinet.FR ", ProfileErrorCode.VALIDATION_ERROR, "Email"))
                .isEqualTo("dr.martin@cabinet.fr");
        assertThatThrownBy(() -> ContactFormats.email("pas-un-email", ProfileErrorCode.VALIDATION_ERROR, "Email"))
                .isInstanceOf(ProfileException.class);
    }

    @Test
    @DisplayName("Seules les URL absolues http(s) sont acceptees")
    void httpUrlAcceptsOnlyAbsoluteHttpUrls() {
        assertThat(ContactFormats.httpUrl("https://cabinet.example.fr/equipe", ProfileErrorCode.VALIDATION_ERROR,
                "Site web")).isEqualTo("https://cabinet.example.fr/equipe");
        assertThatThrownBy(() -> ContactFormats.httpUrl("javascript:alert(1)", ProfileErrorCode.VALIDATION_ERROR,
                "Site web")).isInstanceOf(ProfileException.class);
        assertThatThrownBy(() -> ContactFormats.httpUrl("ftp://cabinet.example.fr", ProfileErrorCode.VALIDATION_ERROR,
                "Site web")).isInstanceOf(ProfileException.class);
    }

    @Test
    @DisplayName("La longueur d'une URL est bornee")
    void httpUrlIsBoundedInLength() {
        String tooLong = "https://example.fr/" + "a".repeat(500);
        assertThatThrownBy(() -> ContactFormats.httpUrl(tooLong, ProfileErrorCode.VALIDATION_ERROR, "Site web"))
                .isInstanceOf(ProfileException.class);
    }
}
