package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.model.user.AdminType;

import java.util.List;

/** Gestion administrative des comptes (liste, suspension / reactivation, administrateurs systeme). */
public interface UserAdministrationUseCase {

    Views.PagedResult<Views.UserView> listUsers(int page, int size);

    Views.UserView changeAccountStatus(AdminCommands.ChangeAccountStatus command);

    /** Detail d'un compte utilisateur. */
    Views.UserView getUser(Long userId);

    /** Deverrouille un compte verrouille apres des tentatives de connexion echouees. */
    Views.UserView unlockAccount(Long userId);

    /** Cree un compte administrateur systeme (SUPER_ADMIN, MODERATOR, TECHNICAL_SUPPORT, COMMERCIAL_SUPPORT, DATA_ANALYST). */
    Views.UserView createAdministrator(AdminCommands.CreateAdministrator command);

    /** Liste les comptes administrateurs systeme (filtrable par type). */
    List<Views.UserView> listAdministrators(AdminType adminType);

    /** Attribue / remplace les permissions granulaires directes d'un utilisateur. */
    Views.UserView updateDirectPermissions(AdminCommands.UpdateDirectPermissions command);

    /** Revocation instantanee de tous les jetons JWT, refresh tokens et sessions d'un utilisateur. */
    void revokeUserTokens(Long userId);
}
