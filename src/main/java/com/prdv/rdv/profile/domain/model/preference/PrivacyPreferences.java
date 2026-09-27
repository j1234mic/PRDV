package com.prdv.rdv.profile.domain.model.preference;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Agregat racine « Preferences &amp; confidentialite » (module 2.1).
 *
 * <p>Porte les langues preferees, les besoins d'accessibilite (malvoyant,
 * sourd...), les preferences de communication, les consentements RGPD
 * (avec preuve : version de politique, IP, horodatage), la granularite de
 * visibilite « qui peut voir quoi », le partage avec le DMP national et la
 * demande d'effacement.
 */
@Getter
@Setter
public class PrivacyPreferences {

    // ------------------------------------------------------------------
    // Enumerations
    // ------------------------------------------------------------------

    public enum AccessibilityNeed {
        VISUALLY_IMPAIRED, HEARING_IMPAIRED, MOBILITY_IMPAIRED, COGNITIVE, SPEECH_DISORDER, NONE
    }

    public enum CommunicationChannel { EMAIL, SMS, PHONE_CALL, APP_PUSH, POSTAL_MAIL }

    public enum ConsentPurpose {
        MARKETING, RESEARCH, CONNECTED_DEVICES, DMP_SHARING, THIRD_PARTY_SHARING, ANALYTICS
    }

    /** Grandes categories de donnees soumises a une regle de visibilite. */
    public enum DataCategory {
        IDENTITY, MEDICAL_RECORD, MEDICAL_DOCUMENTS, CONNECTED_HEALTH, APPOINTMENTS
    }

    public enum VisibilityLevel {
        /** Personne d'autre que le patient. */
        PRIVATE,
        /** Les praticiens qui suivent effectivement le patient. */
        MY_PRACTITIONERS,
        /** Une liste nominative de praticiens autorises. */
        SPECIFIC_PRACTITIONERS,
        /** Tout professionnel de sante de la plateforme. */
        ALL_PRACTITIONERS
    }

    // ------------------------------------------------------------------
    // Value Objects
    // ------------------------------------------------------------------

    /** Preuve de consentement : quoi, quand, sous quelle version de politique, depuis quelle IP. */
    public record ConsentRecord(ConsentPurpose purpose,
                                boolean granted,
                                String policyVersion,
                                Instant recordedAt,
                                Instant withdrawnAt,
                                String ipAddress) {

        public boolean isInForce() {
            return granted && withdrawnAt == null;
        }
    }

    /** Regle de visibilite par categorie de donnees (granularite « qui peut voir quoi »). */
    public record VisibilityRule(DataCategory category, VisibilityLevel level, Set<Long> granteeUserIds) {

        public VisibilityRule {
            if (category == null || level == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Une regle de visibilite exige une categorie et un niveau");
            }
            granteeUserIds = granteeUserIds == null ? Set.of() : Set.copyOf(granteeUserIds);
            if (level == VisibilityLevel.SPECIFIC_PRACTITIONERS && granteeUserIds.isEmpty()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le niveau SPECIFIC_PRACTITIONERS exige au moins un beneficiaire");
            }
        }
    }

    public record AccessibilitySettings(Set<AccessibilityNeed> needs,
                                        boolean screenReaderOptimized,
                                        boolean largePrint,
                                        String signLanguage,
                                        boolean subtitlesRequired) {

        public AccessibilitySettings {
            needs = needs == null ? Set.of() : Set.copyOf(needs);
        }

        public static AccessibilitySettings none() {
            return new AccessibilitySettings(Set.of(), false, false, null, false);
        }
    }

    public record CommunicationSettings(Set<CommunicationChannel> preferredChannels,
                                        boolean quietHoursEnabled,
                                        LocalTime quietHoursStart,
                                        LocalTime quietHoursEnd) {

        public CommunicationSettings {
            preferredChannels = preferredChannels == null ? Set.of() : Set.copyOf(preferredChannels);
            if (quietHoursEnabled && (quietHoursStart == null || quietHoursEnd == null)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Les horaires de tranquillite exigent une heure de debut et de fin");
            }
        }

        public static CommunicationSettings defaults() {
            return new CommunicationSettings(Set.of(CommunicationChannel.EMAIL), false, null, null);
        }
    }

    // ------------------------------------------------------------------
    // Etat de l'agregat
    // ------------------------------------------------------------------

    public static final String DEFAULT_LANGUAGE = "fr";

    private Long id;
    private Long userId;

    private List<String> preferredLanguages = new ArrayList<>(List.of(DEFAULT_LANGUAGE));
    private String primaryLanguage = DEFAULT_LANGUAGE;

    private AccessibilitySettings accessibility = AccessibilitySettings.none();
    private CommunicationSettings communication = CommunicationSettings.defaults();

    private List<ConsentRecord> consentHistory = new ArrayList<>();
    private List<VisibilityRule> visibilityRules = new ArrayList<>();

    private boolean dmpSharingEnabled;
    private Instant erasureRequestedAt;

    private Instant createdAt;
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Fabrique
    // ------------------------------------------------------------------

    public static PrivacyPreferences defaults(Long userId, Clock clock) {
        if (userId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Les preferences doivent etre rattachees a un utilisateur");
        }
        PrivacyPreferences preferences = new PrivacyPreferences();
        preferences.userId = userId;
        preferences.createdAt = clock.instant();
        preferences.updatedAt = preferences.createdAt;
        for (DataCategory category : DataCategory.values()) {
            preferences.visibilityRules.add(new VisibilityRule(category,
                    category == DataCategory.IDENTITY ? VisibilityLevel.MY_PRACTITIONERS
                            : VisibilityLevel.PRIVATE, Set.of()));
        }
        return preferences;
    }

    // ------------------------------------------------------------------
    // Comportements : langues et accessibilite
    // ------------------------------------------------------------------

    public void updateLanguages(List<String> languages, String primary, Clock clock) {
        List<String> normalized = new ArrayList<>();
        if (languages != null) {
            for (String language : languages) {
                if (language != null && !language.isBlank()) {
                    String code = language.trim().toLowerCase(Locale.ROOT);
                    if (!normalized.contains(code)) {
                        normalized.add(code);
                    }
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(DEFAULT_LANGUAGE);
        }
        String normalizedPrimary = primary == null || primary.isBlank()
                ? normalized.get(0)
                : primary.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(normalizedPrimary)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La langue principale doit figurer parmi les langues preferees");
        }
        this.preferredLanguages = normalized;
        this.primaryLanguage = normalizedPrimary;
        touch(clock);
    }

    public void updateAccessibility(AccessibilitySettings settings, Clock clock) {
        this.accessibility = settings == null ? AccessibilitySettings.none() : settings;
        touch(clock);
    }

    public void updateCommunication(CommunicationSettings settings, Clock clock) {
        this.communication = settings == null ? CommunicationSettings.defaults() : settings;
        touch(clock);
    }

    // ------------------------------------------------------------------
    // Comportements : consentements RGPD
    // ------------------------------------------------------------------

    /** Enregistre un consentement (ou son retrait) avec sa preuve. */
    public ConsentRecord recordConsent(ConsentPurpose purpose, boolean granted, String policyVersion,
                                       String ipAddress, Clock clock) {
        if (purpose == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La finalite du consentement est obligatoire");
        }
        ConsentRecord record = new ConsentRecord(purpose, granted, policyVersion, clock.instant(),
                granted ? null : clock.instant(), ipAddress);
        this.consentHistory.add(record);
        if (purpose == ConsentPurpose.DMP_SHARING) {
            this.dmpSharingEnabled = granted;
        }
        touch(clock);
        return record;
    }

    public ConsentRecord withdrawConsent(ConsentPurpose purpose, String ipAddress, Clock clock) {
        ConsentRecord current = consentFor(purpose)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Aucun consentement en cours pour la finalite " + purpose));
        if (!current.isInForce()) {
            throw ProfileException.of(ProfileErrorCode.CONSENT_ALREADY_WITHDRAWN,
                    "Le consentement " + purpose + " est deja retire");
        }
        return recordConsent(purpose, false, current.policyVersion(), ipAddress, clock);
    }

    public Optional<ConsentRecord> consentFor(ConsentPurpose purpose) {
        ConsentRecord latest = null;
        for (ConsentRecord record : consentHistory) {
            if (record.purpose() == purpose) {
                latest = record;
            }
        }
        return Optional.ofNullable(latest);
    }

    public boolean hasConsent(ConsentPurpose purpose) {
        return consentFor(purpose).map(ConsentRecord::isInForce).orElse(false);
    }

    public List<ConsentRecord> activeConsents() {
        return consentHistory.stream().filter(ConsentRecord::isInForce).toList();
    }

    // ------------------------------------------------------------------
    // Comportements : granularite de visibilite
    // ------------------------------------------------------------------

    public VisibilityRule setVisibility(DataCategory category, VisibilityLevel level,
                                        Set<Long> granteeUserIds, Clock clock) {
        VisibilityRule rule = new VisibilityRule(category, level, granteeUserIds);
        this.visibilityRules.removeIf(existing -> existing.category() == category);
        this.visibilityRules.add(rule);
        touch(clock);
        return rule;
    }

    public VisibilityRule ruleFor(DataCategory category) {
        return visibilityRules.stream()
                .filter(rule -> rule.category() == category)
                .findFirst()
                .orElseGet(() -> new VisibilityRule(category, VisibilityLevel.PRIVATE, Set.of()));
    }

    /**
     * Le demandeur peut-il consulter cette categorie de donnees ?
     *
     * @param myPractitionerUserIds praticiens qui suivent actuellement le patient
     *                              (calcule par la couche application a partir des rendez-vous)
     */
    public boolean allowsAccess(DataCategory category, Long requesterUserId, Set<Long> myPractitionerUserIds) {
        if (requesterUserId == null) {
            return false;
        }
        if (requesterUserId.equals(userId)) {
            return true;
        }
        VisibilityRule rule = ruleFor(category);
        return switch (rule.level()) {
            case PRIVATE -> false;
            case MY_PRACTITIONERS -> myPractitionerUserIds != null && myPractitionerUserIds.contains(requesterUserId);
            case SPECIFIC_PRACTITIONERS -> rule.granteeUserIds().contains(requesterUserId);
            case ALL_PRACTITIONERS -> true;
        };
    }

    public void updateDmpSharing(boolean enabled, String policyVersion, String ipAddress, Clock clock) {
        recordConsent(ConsentPurpose.DMP_SHARING, enabled, policyVersion, ipAddress, clock);
    }

    // ------------------------------------------------------------------
    // RGPD
    // ------------------------------------------------------------------

    public void requestErasure(Clock clock) {
        this.erasureRequestedAt = clock.instant();
        touch(clock);
    }

    /** Revocation globale : tout redevient prive et tous les consentements sont retires. */
    public void revokeEverything(String ipAddress, Clock clock) {
        for (ConsentPurpose purpose : ConsentPurpose.values()) {
            if (hasConsent(purpose)) {
                recordConsent(purpose, false, "erasure", ipAddress, clock);
            }
        }
        for (DataCategory category : DataCategory.values()) {
            setVisibility(category, VisibilityLevel.PRIVATE, Set.of(), clock);
        }
        this.dmpSharingEnabled = false;
        requestErasure(clock);
    }

    // ------------------------------------------------------------------

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    /** Ensemble deduplique utilitaire (ordre de declaration preserve). */
    public static Set<CommunicationChannel> channels(CommunicationChannel... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
