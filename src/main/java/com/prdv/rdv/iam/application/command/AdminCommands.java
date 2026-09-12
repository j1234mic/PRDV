package com.prdv.rdv.iam.application.command;

import java.util.Set;

/**
 * Commandes d'administration : roles, validation des dossiers, revue KYC,
 * suspension de comptes, delegations.
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
}
