package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.PrivacyPreferencesRepository;
import com.prdv.rdv.profile.application.port.output.TreatingPractitionersPort;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Set;

/**
 * Garde d'acces aux donnees de sante (Single Responsibility).
 *
 * <p>Toute lecture croisee (un praticien consulte le dossier d'un patient)
 * passe par ici : le controle s'appuie sur les regles de granularite du
 * patient (« qui peut voir quoi ») et sur la liste des praticiens qui le
 * suivent. Le refus est trace cote service appelant.
 */
@Component
public class ProfileAccessGuard {

    private final PrivacyPreferencesRepository preferencesRepository;
    private final TreatingPractitionersPort treatingPractitioners;
    private final CurrentUserPort currentUser;
    private final Clock clock;

    public ProfileAccessGuard(PrivacyPreferencesRepository preferencesRepository,
                              TreatingPractitionersPort treatingPractitioners,
                              CurrentUserPort currentUser,
                              Clock clock) {
        this.preferencesRepository = preferencesRepository;
        this.treatingPractitioners = treatingPractitioners;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /** Preferences du patient, initialisees aux valeurs par defaut si absentes. */
    public PrivacyPreferences preferencesOf(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> preferencesRepository.save(PrivacyPreferences.defaults(userId, clock)));
    }

    public Long requireCurrentUser() {
        return currentUser.requireCurrentUserId();
    }

    public boolean isSelf(Long patientUserId) {
        return currentUser.currentUserId().map(id -> id.equals(patientUserId)).orElse(false);
    }

    /**
     * Identifiant effectif de la cible : {@code null} designe l'utilisateur
     * connecte (convention utilisee par les endpoints « mes donnees »).
     */
    public Long resolveTarget(Long patientUserId) {
        return patientUserId == null ? requireCurrentUser() : patientUserId;
    }

    /**
     * Verifie que l'utilisateur connecte peut acceder a cette categorie de
     * donnees du patient vise. Un identifiant nul designe l'utilisateur
     * connecte lui-meme, toujours autorise sur ses propres donnees.
     */
    public void requireAccess(Long patientUserId, PrivacyPreferences.DataCategory category) {
        Long requester = requireCurrentUser();
        if (patientUserId == null || requester.equals(patientUserId)) {
            return;
        }
        PrivacyPreferences preferences = preferencesRepository.findByUserId(patientUserId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.PROFILE_NOT_FOUND,
                        "Aucun profil de confidentialite pour l'utilisateur " + patientUserId));
        Set<Long> treating = treatingPractitioners.practitionerUserIdsOf(patientUserId);
        if (!preferences.allowsAccess(category, requester, treating)) {
            throw ProfileException.of(ProfileErrorCode.CONSENT_REQUIRED,
                    "Le patient n'autorise pas le partage de la categorie " + category
                            + " (niveau actuel : " + preferences.ruleFor(category).level() + ")");
        }
    }

    /** Le consentement a une finalite est-il en cours ? */
    public boolean hasConsent(Long userId, PrivacyPreferences.ConsentPurpose purpose) {
        return preferencesRepository.findByUserId(userId)
                .map(preferences -> preferences.hasConsent(purpose))
                .orElse(false);
    }
}
