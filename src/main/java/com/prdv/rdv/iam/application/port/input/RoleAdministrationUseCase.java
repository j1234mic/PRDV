package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/** Administration RBAC : roles predefinis/personnalises et attribution. */
public interface RoleAdministrationUseCase {

    Views.RoleView createRole(AdminCommands.CreateRole command);

    List<Views.RoleView> listRoles();

    List<Views.PermissionView> listPermissions();

    /** Permissions d'un module donne (ex. {@code iam}, {@code appointment}), insensible a la casse. */
    List<Views.PermissionView> listPermissionsByModule(String module);

    void assignRoles(AdminCommands.AssignRoles command);

    Views.RoleView updateRole(AdminCommands.UpdateRole command);

    void deleteCustomRole(String roleName);
}
