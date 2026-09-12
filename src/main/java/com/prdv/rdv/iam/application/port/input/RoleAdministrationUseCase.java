package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/** Administration RBAC : roles predefinis/personnalises et attribution. */
public interface RoleAdministrationUseCase {

    Views.RoleView createRole(AdminCommands.CreateRole command);

    List<Views.RoleView> listRoles();

    List<Views.PermissionView> listPermissions();

    void assignRoles(AdminCommands.AssignRoles command);
}
