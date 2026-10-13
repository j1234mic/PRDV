package com.prdv.rdv.iam.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public final class SecretaryDtos {

    private SecretaryDtos() {
    }

    public record RegisterSecretaryRequest(
            @NotBlank @Email String email,
            @NotBlank String firstName,
            @NotBlank String lastName,
            Set<Long> supervisedPractitionerIds,
            Set<Long> accessibleEstablishmentIds,
            /** Permissions granulaires (ex : SECRETARY_SCHEDULE_READ, SECRETARY_SCHEDULE_WRITE). */
            Set<String> permissionCodes) {

        public RegisterSecretaryRequest(String email,
                                        String firstName,
                                        String lastName,
                                        Set<Long> supervisedPractitionerIds,
                                        Set<String> permissionCodes) {
            this(email, firstName, lastName, supervisedPractitionerIds, Set.of(), permissionCodes);
        }
    }

    public record UpdateSecretaryPermissionsRequest(
            @NotNull Set<String> permissionCodes) {
    }

    public record UpdateSecretaryAssignmentsRequest(
            Set<Long> supervisedPractitionerIds,
            Set<Long> accessibleEstablishmentIds) {
    }
}
