package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.result.Views;

/** Gestion administrative des comptes (liste, suspension / reactivation). */
public interface UserAdministrationUseCase {

    Views.PagedResult<Views.UserView> listUsers(int page, int size);

    Views.UserView changeAccountStatus(AdminCommands.ChangeAccountStatus command);
}
