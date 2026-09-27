package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.PatientIdentityCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

/**
 * Cas d'usage : gestion de l'identite patient (etat civil, coordonnees,
 * couverture sociale, carte Vitale, DMP).
 */
public interface PatientIdentityUseCase {

    ProfileViews.PatientIdentityView myIdentity();

    ProfileViews.PatientIdentityView identityOf(Long patientUserId);

    ProfileViews.PatientIdentityView updateCivilStatus(PatientIdentityCommands.UpdateCivilStatus command);

    ProfileViews.PatientIdentityView replaceContacts(PatientIdentityCommands.ReplaceContacts command);

    ProfileViews.PatientIdentityView replaceAddresses(PatientIdentityCommands.ReplaceAddresses command);

    ProfileViews.PatientIdentityView updateEmergencyContact(PatientIdentityCommands.UpdateEmergencyContact command);

    ProfileViews.PatientIdentityView declareTreatingPhysician(
            PatientIdentityCommands.DeclareTreatingPhysician command);

    ProfileViews.PatientIdentityView registerSocialSecurityNumber(
            PatientIdentityCommands.RegisterSocialSecurityNumber command);

    ProfileViews.PatientIdentityView updateInsurance(PatientIdentityCommands.UpdateInsurance command);

    ProfileViews.PatientIdentityView uploadProfilePhoto(PatientIdentityCommands.StoreFile command);

    ProfileViews.PatientIdentityView uploadIdentityDocument(PatientIdentityCommands.StoreFile command);

    ProfileViews.PatientIdentityView registerVitaleCard(PatientIdentityCommands.RegisterVitaleCard command);

    ProfileViews.PatientIdentityView linkDmp(PatientIdentityCommands.LinkDmp command);

    ProfileViews.PatientIdentityView updateDmpSharing(PatientIdentityCommands.UpdateDmpSharing command);

    /** Rapatrie depuis le DMP national les elements disponibles (via la passerelle). */
    ProfileViews.DmpAccountView synchronizeDmp();
}
