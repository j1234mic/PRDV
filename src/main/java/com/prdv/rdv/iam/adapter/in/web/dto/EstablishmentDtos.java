package com.prdv.rdv.iam.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Requetes etablissements de sante et decisions sur les rattachements.
 */
public final class EstablishmentDtos {

    private EstablishmentDtos() {
    }

    public record RegisterEstablishmentRequest(
            @NotBlank @Email String email,
            String phone,
            @NotBlank String password,
            @NotBlank String legalName,
            @NotBlank String siret,
            String address,
            List<String> departments) {
    }

    public record MembershipDecisionRequest(@NotNull boolean approved) {
    }
}
