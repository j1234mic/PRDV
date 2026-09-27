package com.prdv.rdv.profile.domain.model.practitioner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les badges « verifie », « populaire » et « nouveau » sont derives des regles
 * du domaine : jamais saisis, jamais stockes.
 */
class BadgePolicyTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private static final Instant ACCOUNT_CREATED = CLOCK.instant().minus(Duration.ofDays(400));

    @Test
    @DisplayName("Le badge VERIFIE exige identite, diplomes, assurance et RIB verifies")
    void verifiedBadgeRequiresEveryVerification() {
        BadgePolicy.VerificationState complete = new BadgePolicy.VerificationState(true, true, true, true);

        List<Badge> badges = BadgePolicy.evaluate(publishableDossier(), RatingSummary.empty(), complete,
                ACCOUNT_CREATED, CLOCK);

        assertThat(badges).extracting(Badge::type).contains(Badge.BadgeType.VERIFIED);

        List<Badge> partial = BadgePolicy.evaluate(publishableDossier(), RatingSummary.empty(),
                new BadgePolicy.VerificationState(true, true, true, false), ACCOUNT_CREATED, CLOCK);

        assertThat(partial).extracting(Badge::type).doesNotContain(Badge.BadgeType.VERIFIED);
    }

    @Test
    @DisplayName("Le badge POPULAIRE exige au moins 20 avis avec 4,5/5 de moyenne")
    void popularBadgeNeedsVolumeAndQuality() {
        RatingSummary eligible = new RatingSummary(20, new BigDecimal("4.50"), Map.of());
        RatingSummary notEnoughReviews = new RatingSummary(19, new BigDecimal("4.90"), Map.of());
        RatingSummary notGoodEnough = new RatingSummary(60, new BigDecimal("4.49"), Map.of());

        assertThat(BadgePolicy.evaluate(publishableDossier(), eligible,
                BadgePolicy.VerificationState.none(), ACCOUNT_CREATED, CLOCK))
                .extracting(Badge::type).contains(Badge.BadgeType.POPULAR);
        assertThat(BadgePolicy.evaluate(publishableDossier(), notEnoughReviews,
                BadgePolicy.VerificationState.none(), ACCOUNT_CREATED, CLOCK))
                .extracting(Badge::type).doesNotContain(Badge.BadgeType.POPULAR);
        assertThat(BadgePolicy.evaluate(publishableDossier(), notGoodEnough,
                BadgePolicy.VerificationState.none(), ACCOUNT_CREATED, CLOCK))
                .extracting(Badge::type).doesNotContain(Badge.BadgeType.POPULAR);
    }

    @Test
    @DisplayName("Le badge NOUVEAU couvre les 90 premiers jours puis expire")
    void newBadgeExpiresAfterNinetyDays() {
        Instant recent = CLOCK.instant().minus(Duration.ofDays(10));
        Instant old = CLOCK.instant().minus(Duration.ofDays(100));

        List<Badge> recentBadges = BadgePolicy.evaluate(publishableDossier(), RatingSummary.empty(),
                BadgePolicy.VerificationState.none(), recent, CLOCK);

        assertThat(recentBadges).extracting(Badge::type).contains(Badge.BadgeType.NEW);
        Badge nouveau = recentBadges.stream()
                .filter(badge -> badge.type() == Badge.BadgeType.NEW)
                .findFirst()
                .orElseThrow();
        assertThat(nouveau.expiresAt()).isEqualTo(recent.plus(Duration.ofDays(90)));

        assertThat(BadgePolicy.evaluate(publishableDossier(), RatingSummary.empty(),
                BadgePolicy.VerificationState.none(), old, CLOCK))
                .extracting(Badge::type).doesNotContain(Badge.BadgeType.NEW);
    }

    @Test
    @DisplayName("Un dossier non publiable ne beneficie d'aucune mise en avant")
    void unpublishableDossierGetsNoBadge() {
        PractitionerDossier dossier = publishableDossier();
        dossier.updateIdentity(identityWithoutSpecialty(), CLOCK);
        assertThat(dossier.isPublishable()).isFalse();

        List<Badge> badges = BadgePolicy.evaluate(dossier,
                new RatingSummary(50, new BigDecimal("5.00"), Map.of()),
                new BadgePolicy.VerificationState(true, true, true, true), ACCOUNT_CREATED, CLOCK);

        assertThat(badges).isEmpty();
    }

    @Test
    @DisplayName("La synthese des avis est calculee a la volee, sans donnee stockee")
    void ratingSummaryIsDerived() {
        PractitionerRating five = rating(5);
        PractitionerRating four = rating(4);

        RatingSummary summary = RatingSummary.of(List.of(five, four));

        assertThat(summary.reviewCount()).isEqualTo(2);
        assertThat(summary.averageScore()).isEqualByComparingTo("4.50");
        assertThat(summary.distribution()).containsEntry(5, 1).containsEntry(4, 1).containsEntry(1, 0);
        assertThat(RatingSummary.of(List.of()).isEmpty()).isTrue();
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private static PractitionerRating rating(int score) {
        PractitionerRating rating = new PractitionerRating();
        rating.setPractitionerUserId(7L);
        rating.setPatientUserId(100L + score);
        rating.setScore(score);
        rating.setCreatedAt(CLOCK.instant());
        return rating;
    }

    private static PractitionerDossier publishableDossier() {
        PractitionerDossier dossier = PractitionerDossier.create(7L, identity("Cardiologie"), CLOCK);
        dossier.updatePracticeInformation(new PractitionerDossier.PracticeInformation(
                PractitionerDossier.ConventionSector.SECTOR_1, List.of(), false, false, null,
                Set.of(PractitionerDossier.PaymentMethod.CREDIT_CARD), true, "CMU et AME acceptes",
                List.of("Consultation", "ECG"), List.of("Electrocardiographe"), List.of("Hypertension"),
                new PractitionerDossier.AgeRange(12, 100), true, "Plateforme PRDV"), CLOCK);
        return dossier;
    }

    private static PractitionerDossier.ProfessionalIdentity identity(String mainSpecialty) {
        return new PractitionerDossier.ProfessionalIdentity(PractitionerDossier.Title.DOCTOR, "Camille",
                "Durand", "10100000001", null, PractitionerDossier.RegistrationOrder.PHYSICIANS,
                mainSpecialty, List.of("Medecine vasculaire"), List.of(), List.of("Echographie"),
                List.of(new PractitionerDossier.Diploma("Doctorat en medecine", "Universite de Lille",
                        2012, "Cardiologie")),
                14, List.of("fr", "en"), null, null, "Cardiologue interventionnel");
    }

    private static PractitionerDossier.ProfessionalIdentity identityWithoutSpecialty() {
        return new PractitionerDossier.ProfessionalIdentity(PractitionerDossier.Title.DOCTOR, "Camille",
                "Durand", "10100000001", null, PractitionerDossier.RegistrationOrder.PHYSICIANS,
                null, List.of(), List.of(), List.of(), List.of(), 14, List.of("fr"), null, null, null);
    }
}
