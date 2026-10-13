package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.user.AdminType;

import java.util.Set;

/**
 * Commandes d'administration : roles, validation des dossiers, revue KYC,
 * suspension de comptes, delegations, creation d'administrateurs systeme.
 */
public final class AdminCommands {

    private AdminCommands() {
    }

    public record CreateRole(String name, String description, Set<String> permissionCodes) {
    }

    public record AssignRoles(Long userId, Set<String> roleNames) {
    }

    public record ReviewApplication(Long userId, boolean approved, String reason) {
    }

    public record ReviewDocument(Long documentId, boolean approved, String note) {
    }

    public record ChangeAccountStatus(Long userId, boolean suspend) {
    }

    public record GrantDelegation(Long granteeUserId,
                                  Set<String> permissionCodes,
                                  String reason,
                                  java.time.Instant validFrom,
                                  java.time.Instant validUntil) {
    }

    /** Creation d'un administrateur systeme (Super admin, Moderateur, Support tech/commercial, Analyste). */
    public record CreateAdministrator(String email, String phone, String password, AdminType adminType) {
    }

    /** Mise a jour des permissions d'un role personnalise. */
    public record UpdateRole(String roleName, String description, Set<String> permissionCodes) {
    }

    /** Attribution directe de permissions granulaires a un utilisateur. */
    public record UpdateDirectPermissions(Long userId, Set<String> permissionCodes) {
    }
}
