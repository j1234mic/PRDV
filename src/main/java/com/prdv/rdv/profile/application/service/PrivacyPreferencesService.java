package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.PrivacyCommands;
import com.prdv.rdv.profile.application.port.input.PrivacyPreferencesUseCase;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.DmpGatewayPort;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.application.port.output.PrivacyPreferencesRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAccessGuard;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.config.ProfileProperties;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Cas d'usage : preferences et confidentialite (module 2.1).
 *
 * <p>Chaque consentement est enregistre avec sa preuve (version de politique,
 * horodatage, IP) et l'historique complet est conserve : c'est ce qui permet
 * de demontrer la conformite RGPD (art. 7) et de dater un retrait.
 */
@Service
public class PrivacyPreferencesService implements PrivacyPreferencesUseCase {

    private final PrivacyPreferencesRepository preferencesRepository;
    private final PatientIdentityRepository identityRepository;
    private final DmpGatewayPort dmpGateway;
    private final CurrentUserPort currentUser;
    private final ProfileAccessGuard accessGuard;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final ProfileProperties properties;
    private final Clock clock;

    public PrivacyPreferencesService(PrivacyPreferencesRepository preferencesRepository,
                                     PatientIdentityRepository identityRepository,
                                     DmpGatewayPort dmpGateway,
                                     CurrentUserPort currentUser,
                                     ProfileAccessGuard accessGuard,
                                     ProfileViewMapper viewMapper,
                                     ProfileAuditTrail auditTrail,
                                     ProfileEventPublisher eventPublisher,
                                     ProfileProperties properties,
                                     Clock clock) {
        this.preferencesRepository = preferencesRepository;
        this.identityRepository = identityRepository;
        this.dmpGateway = dmpGateway;
        this.currentUser = currentUser;
        this.accessGuard = accessGuard;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProfileViews.PrivacyPreferencesView myPreferences() {
        return viewMapper.preferencesView(accessGuard.preferencesOf(currentUser.requireCurrentUserId()));
    }

    @Override
    @Transactional
    public ProfileViews.PrivacyPreferencesView update(PrivacyCommands.UpdatePreferences command) {
        Long userId = currentUser.requireCurrentUserId();
        PrivacyPreferences preferences = accessGuard.preferencesOf(userId);

        preferences.updateLanguages(command.preferredLanguages(), command.primaryLanguage(), clock);
        if (command.accessibility() != null) {
            preferences.updateAccessibility(new PrivacyPreferences.AccessibilitySettings(
                    command.accessibility().needs(), command.accessibility().screenReaderOptimized(),
                    command.accessibility().largePrint(), command.accessibility().signLanguage(),
                    command.accessibility().subtitlesRequired()), clock);
        }
        if (command.communication() != null) {
            preferences.updateCommunication(new PrivacyPreferences.CommunicationSettings(
                    command.communication().preferredChannels(),
                    command.communication().quietHoursEnabled(),
                    command.communication().quietHoursStart(),
                    command.communication().quietHoursEnd()), clock);
        }
        return viewMapper.preferencesView(saveAndAudit(preferences, "preferences mises a jour"));
    }

    @Override
    @Transactional
    public ProfileViews.PrivacyPreferencesView recordConsent(PrivacyCommands.RecordConsent command) {
        Long userId = currentUser.requireCurrentUserId();
        PrivacyPreferences preferences = accessGuard.preferencesOf(userId);
        String policyVersion = command.policyVersion() == null
                ? properties.getConsent().getPolicyVersion()
                : command.policyVersion();
        preferences.recordConsent(command.purpose(), command.granted(), policyVersion,
                command.ipAddress(), clock);
        return saveConsent(preferences, command.purpose(), command.granted());
    }

    @Override
    @Transactional
    public ProfileViews.PrivacyPreferencesView withdrawConsent(PrivacyCommands.WithdrawConsent command) {
        Long userId = currentUser.requireCurrentUserId();
        PrivacyPreferences preferences = accessGuard.preferencesOf(userId);
        preferences.withdrawConsent(command.purpose(), command.ipAddress(), clock);
        return saveConsent(preferences, command.purpose(), false);
    }

    @Override
    @Transactional
    public ProfileViews.PrivacyPreferencesView setVisibility(PrivacyCommands.SetVisibility command) {
        Long userId = currentUser.requireCurrentUserId();
        PrivacyPreferences preferences = accessGuard.preferencesOf(userId);
        preferences.setVisibility(command.category(), command.level(), command.granteeUserIds(), clock);
        PrivacyPreferences saved = saveAndAudit(preferences,
                "visibilite " + command.category() + " -> " + command.level());
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.VISIBILITY_UPDATED, userId,
                "PrivacyPreferences", String.valueOf(saved.getId()),
                command.category() + " = " + command.level());
        return viewMapper.preferencesView(saved);
    }

    @Override
    @Transactional
    public ProfileViews.PrivacyPreferencesView updateDmpSharing(PrivacyCommands.UpdateDmpSharing command) {
        Long userId = currentUser.requireCurrentUserId();
        PrivacyPreferences preferences = accessGuard.preferencesOf(userId);
        String policyVersion = command.policyVersion() == null
                ? properties.getConsent().getPolicyVersion()
                : command.policyVersion();
        preferences.updateDmpSharing(command.enabled(), policyVersion, command.ipAddress(), clock);

        // Le rattachement DMP cote identite patient reste coherent avec le consentement.
        Optional<PatientIdentity> identity = identityRepository.findByUserId(userId);
        identity.filter(patient -> patient.getDmpAccount() != null && patient.getDmpAccount().linked())
                .ifPresent(patient -> {
                    patient.updateDmpSharing(command.enabled(), clock);
                    identityRepository.save(patient);
                    if (!command.enabled()) {
                        dmpGateway.revokeSharing(userId, patient.getDmpAccount().dmpIdentifier());
                    }
                });

        return saveConsent(preferences, PrivacyPreferences.ConsentPurpose.DMP_SHARING, command.enabled());
    }

    // ------------------------------------------------------------------

    private ProfileViews.PrivacyPreferencesView saveConsent(PrivacyPreferences preferences,
                                                            PrivacyPreferences.ConsentPurpose purpose,
                                                            boolean granted) {
        PrivacyPreferences saved = saveAndAudit(preferences,
                "consentement " + purpose + " " + (granted ? "accorde" : "retire"));
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.CONSENT_RECORDED, preferences.getUserId(),
                "Consent", purpose.name(), "version " + properties.getConsent().getPolicyVersion());
        eventPublisher.publish(new ProfileEvent.ConsentRecorded(preferences.getUserId(), purpose.name(),
                granted, clock.instant()));
        return viewMapper.preferencesView(saved);
    }

    private PrivacyPreferences saveAndAudit(PrivacyPreferences preferences, String detail) {
        PrivacyPreferences saved = preferencesRepository.save(preferences);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PATIENT_PROFILE_UPDATED,
                saved.getUserId(), "PrivacyPreferences", String.valueOf(saved.getId()), detail);
        return saved;
    }

    /** Garde-fou : une finalite inconnue est refusee au plus tot. */
    static PrivacyPreferences.ConsentPurpose requirePurpose(String purpose) {
        try {
            return PrivacyPreferences.ConsentPurpose.valueOf(purpose);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Finalite de consentement inconnue : " + purpose);
        }
    }
}
