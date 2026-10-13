package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Inscription et gestion des comptes entreprises / etablissements,
 * ainsi que la validation des rattachements praticiens (et remplaçants).
 */
public interface EstablishmentRegistrationUseCase {

    Views.OtpSentView register(RegistrationCommands.RegisterEstablishment command);

    List<Views.EstablishmentView> listEstablishments();

    Views.EstablishmentView currentProfile();

    List<Views.MembershipView> memberships();

    Views.MembershipView reviewMembership(ProfileCommands.ReviewMembership command);

    void revokeMembership(Long membershipId);

    /** Gestion centralisee des services / departements de l'etablissement. */
    Views.EstablishmentView updateDepartments(ProfileCommands.UpdateDepartments command);

    /** Facturation groupee : emission d'une facture consolidee multi-praticiens. */
    Views.GroupInvoiceView issueGroupInvoice(ProfileCommands.IssueGroupInvoice command);

    /** Facturation groupee : liste des factures consolidees de l'etablissement. */
    List<Views.GroupInvoiceView> listGroupInvoices();

    /** Facturation groupee : reglement d'une facture consolidee. */
    Views.GroupInvoiceView payGroupInvoice(Long invoiceId);

    /** Tableau de bord consolide de l'etablissement de sante. */
    Views.EstablishmentDashboardView consolidatedDashboard();
}
