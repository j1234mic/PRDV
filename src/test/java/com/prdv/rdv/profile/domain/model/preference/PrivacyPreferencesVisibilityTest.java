package com.prdv.rdv.profile.domain.model.preference;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Granularite « qui peut voir quoi » et preuve de consentement RGPD.
 *
 * <p>Par defaut tout est ferme (PRIVATE), sauf l'identite visible des
 * praticiens qui suivent le patient : c'est le reglage le plus protecteur.
 */
class PrivacyPreferencesVisibilityTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private static final Long PATIENT = 42L;
    private static final Long TREATING_PRACTITIONER = 7L;
    private static final Long FOREIGN_PRACTITIONER = 99L;

    @Test
    @DisplayName("Par defaut : identite pour mes praticiens, tout le reste prive")
    void defaultsAreClosedByDefault() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);

        assertThat(preferences.ruleFor(PrivacyPreferences.DataCategory.IDENTITY).level())
                .isEqualTo(PrivacyPreferences.VisibilityLevel.MY_PRACTITIONERS);
        assertThat(preferences.ruleFor(PrivacyPreferences.DataCategory.MEDICAL_RECORD).level())
                .isEqualTo(PrivacyPreferences.VisibilityLevel.PRIVATE);
        assertThat(preferences.ruleFor(PrivacyPreferences.DataCategory.CONNECTED_HEALTH).level())
                .isEqualTo(PrivacyPreferences.VisibilityLevel.PRIVATE);
    }

    @Test
    @DisplayName("Le patient garde toujours acces a ses propres donnees")
    void ownerAlwaysHasAccess() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);

        assertThat(preferences.allowsAccess(PrivacyPreferences.DataCategory.MEDICAL_RECORD, PATIENT,
                Set.of())).isTrue();
        assertThat(preferences.allowsAccess(PrivacyPreferences.DataCategory.MEDICAL_RECORD, null,
                Set.of())).isFalse();
    }

    @Test
    @DisplayName("PRIVE : aucun tiers, meme un praticien qui suit le patient")
    void privateDeniesEveryone() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);

        assertThat(preferences.allowsAccess(PrivacyPreferences.DataCategory.MEDICAL_RECORD,
                TREATING_PRACTITIONER, Set.of(TREATING_PRACTITIONER))).isFalse();
    }

    @Test
    @DisplayName("MES PRATICIENS : seulement ceux qui suivent effectivement le patient")
    void myPractitionersAllowsTreatingOnesOnly() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);
        preferences.setVisibility(PrivacyPreferences.DataCategory.MEDICAL_RECORD,
                PrivacyPreferences.VisibilityLevel.MY_PRACTITIONERS, Set.of(), CLOCK);

        assertThat(preferences.allowsAccess(PrivacyPreferences.DataCategory.MEDICAL_RECORD,
                TREATING_PRACTITIONER, Set.of(TREATING_PRACTITIONER))).isTrue();
        assertThat(preferences.allowsAccess(PrivacyPreferences.DataCategory.MEDICAL_RECORD,
                FOREIGN_PRACTITIONER, Set.of(TREATING_PRACTITIONER))).isFalse();
    }

    @Test
    @DisplayName("PRATICIENS NOMMES : liste nominative obligatoire")
    void specificPractitionersRequiresGrantees() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);

        assertThatThrownBy(() -> preferences.setVisibility(PrivacyPreferences.DataCategory.MEDICAL_DOCUMENTS,
                PrivacyPreferences.VisibilityLevel.SPECIFIC_PRACTITIONERS, Set.of(), CLOCK))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);

        preferences.setVisibility(PrivacyPreferences.DataCategory.MEDICAL_DOCUMENTS,
                PrivacyPreferences.VisibilityLevel.SPECIFIC_PRACTITIONERS, Set.of(TREATING_PRACTITIONER),
                CLOCK);

        assertThat(preferences.allowsAccess(PrivacyPreferences.DataCategory.MEDICAL_DOCUMENTS,
                TREATING_PRACTITIONER, Set.of())).isTrue();
        assertThat(preferences.allowsAccess(PrivacyPreferences.DataCategory.MEDICAL_DOCUMENTS,
                FOREIGN_PRACTITIONER, Set.of())).isFalse();
    }

    @Test
    @DisplayName("Une nouvelle regle remplace l'ancienne pour la meme categorie")
    void settingVisibilityReplacesPreviousRule() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);

        preferences.setVisibility(PrivacyPreferences.DataCategory.MEDICAL_RECORD,
                PrivacyPreferences.VisibilityLevel.ALL_PRACTITIONERS, Set.of(), CLOCK);
        preferences.setVisibility(PrivacyPreferences.DataCategory.MEDICAL_RECORD,
                PrivacyPreferences.VisibilityLevel.PRIVATE, Set.of(), CLOCK);

        assertThat(preferences.getVisibilityRules().stream()
                .filter(rule -> rule.category() == PrivacyPreferences.DataCategory.MEDICAL_RECORD)
                .count()).isEqualTo(1);
        assertThat(preferences.ruleFor(PrivacyPreferences.DataCategory.MEDICAL_RECORD).level())
                .isEqualTo(PrivacyPreferences.VisibilityLevel.PRIVATE);
    }

    @Test
    @DisplayName("Le consentement est horodate avec sa preuve, le retrait est conserve")
    void consentIsProvableAndWithdrawalIsKept() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);

        PrivacyPreferences.ConsentRecord granted = preferences.recordConsent(
                PrivacyPreferences.ConsentPurpose.RESEARCH, true, "2026-09", "10.0.0.1", CLOCK);

        assertThat(granted.isInForce()).isTrue();
        assertThat(granted.policyVersion()).isEqualTo("2026-09");
        assertThat(granted.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(preferences.hasConsent(PrivacyPreferences.ConsentPurpose.RESEARCH)).isTrue();

        Clock later = Clock.fixed(CLOCK.instant().plus(Duration.ofDays(3)), ZoneOffset.UTC);
        preferences.withdrawConsent(PrivacyPreferences.ConsentPurpose.RESEARCH, "10.0.0.2", later);

        assertThat(preferences.hasConsent(PrivacyPreferences.ConsentPurpose.RESEARCH)).isFalse();
        assertThat(preferences.getConsentHistory()).hasSize(2);
        assertThat(preferences.getConsentHistory().get(1).granted()).isFalse();
        assertThat(preferences.getConsentHistory().get(1).recordedAt()).isEqualTo(later.instant());

        assertThatThrownBy(() -> preferences.withdrawConsent(PrivacyPreferences.ConsentPurpose.RESEARCH,
                "10.0.0.3", later))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.CONSENT_ALREADY_WITHDRAWN);
    }

    @Test
    @DisplayName("Le partage DMP est pilote par un consentement explicite")
    void dmpSharingFollowsConsent() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);
        assertThat(preferences.isDmpSharingEnabled()).isFalse();

        preferences.updateDmpSharing(true, "2026-09", "10.0.0.1", CLOCK);
        assertThat(preferences.isDmpSharingEnabled()).isTrue();
        assertThat(preferences.hasConsent(PrivacyPreferences.ConsentPurpose.DMP_SHARING)).isTrue();

        preferences.updateDmpSharing(false, "2026-09", "10.0.0.1", CLOCK);
        assertThat(preferences.isDmpSharingEnabled()).isFalse();
        assertThat(preferences.hasConsent(PrivacyPreferences.ConsentPurpose.DMP_SHARING)).isFalse();
    }

    @Test
    @DisplayName("La revocation globale ferme tout et date la demande d'effacement")
    void revokeEverythingClosesAll() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);
        preferences.recordConsent(PrivacyPreferences.ConsentPurpose.MARKETING, true, "2026-09", "10.0.0.1",
                CLOCK);
        preferences.setVisibility(PrivacyPreferences.DataCategory.MEDICAL_RECORD,
                PrivacyPreferences.VisibilityLevel.ALL_PRACTITIONERS, Set.of(), CLOCK);

        preferences.revokeEverything("10.0.0.9", CLOCK);

        assertThat(preferences.hasConsent(PrivacyPreferences.ConsentPurpose.MARKETING)).isFalse();
        assertThat(preferences.ruleFor(PrivacyPreferences.DataCategory.MEDICAL_RECORD).level())
                .isEqualTo(PrivacyPreferences.VisibilityLevel.PRIVATE);
        assertThat(preferences.isDmpSharingEnabled()).isFalse();
        assertThat(preferences.getErasureRequestedAt()).isEqualTo(CLOCK.instant());
    }
}
