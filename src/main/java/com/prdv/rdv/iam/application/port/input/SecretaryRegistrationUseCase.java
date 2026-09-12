package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Gestion des comptes secretaires medicales rattaches aux praticiens
 * (creation par un praticien / etablissement, droits granulaires,
 * acces multi-cabinets).
 */
public interface SecretaryRegistrationUseCase {

    Views.SecretaryView create(RegistrationCommands.RegisterSecretary command);

    /** Secretaires rattachees a un praticien donne. */
    List<Views.SecretaryView> listForPractitioner(Long practitionerUserId);
}
