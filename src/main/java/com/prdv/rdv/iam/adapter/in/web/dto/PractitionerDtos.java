package com.prdv.rdv.iam.adapter.in.web.dto;

import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Requetes liees aux praticiens, contrats et rattachements.
 */
public final class PractitionerDtos {

    private PractitionerDtos() {
    }

    public record RegisterPractitionerRequest(
            @NotBlank @Email String email,
            String phone,
            @NotBlank String password,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String specialty,
            @NotBlank(message = "numero RPPS obligatoire") String rppsNumber,
            String adeliNumber,
            String iban) {
    }

    public record AcceptContractRequest(@NotBlank String version, @NotBlank String contentHash) {
    }

    public record MembershipRequest(
            @NotNull Long establishmentUserId,
            @NotNull EstablishmentMembership.MemberRole role,
            LocalDate validFrom,
            LocalDate validUntil) {
    }

    public record ReplacementRequest(
            @NotNull Long establishmentUserId,
            LocalDate validFrom,
            LocalDate validUntil) {
    }
}
