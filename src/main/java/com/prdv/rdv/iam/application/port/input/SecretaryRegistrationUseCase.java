package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Gestion des comptes secretaires medicales rattaches aux praticiens
 * (creation par un praticien / etablissement, droits granulaires,
 * acces multi-cabinets, historique des actions / audit trail).
 */
public interface SecretaryRegistrationUseCase {

    Views.SecretaryView create(RegistrationCommands.RegisterSecretary command);

    Views.SecretaryDetailView createWithMultiCabinet(RegistrationCommands.RegisterSecretaryMultiCabinet command);

    Views.SecretaryDetailView currentProfile();

    /** Secretaires rattachees a un praticien donne. */
    List<Views.SecretaryView> listForPractitioner(Long practitionerUserId);

    /** Mise a jour des permissions granulaires de la secretaire. */
    Views.SecretaryDetailView updatePermissions(ProfileCommands.UpdateSecretaryPermissions command);

    /** Mise a jour des praticiens supervises et de l'acces multi-cabinets. */
    Views.SecretaryDetailView updateAssignments(ProfileCommands.UpdateSecretaryAssignments command);

    /** Historique des actions (audit trail) de la secretaire medicale. */
    Views.PagedResult<Views.AuditView> secretaryAuditTrail(Long secretaryUserId, int page, int size);
}
