package com.prdv.rdv.iam.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public final class SecretaryDtos {

    private SecretaryDtos() {
    }

    public record RegisterSecretaryRequest(
            @NotBlank @Email String email,
            @NotBlank String firstName,
            @NotBlank String lastName,
            Set<Long> supervisedPractitionerIds,
            /** Permissions granulaires (ex : appointment.write, agenda.read). */
            Set<String> permissionCodes) {
    }
}
