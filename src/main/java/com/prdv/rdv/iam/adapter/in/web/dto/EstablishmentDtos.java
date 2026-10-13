package com.prdv.rdv.iam.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Requetes etablissements de sante, services/departements, facturation groupee
 * et decisions sur les rattachements.
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

    public record UpdateDepartmentsRequest(@NotNull List<String> departments) {
    }

    public record IssueGroupInvoiceRequest(
            @NotNull LocalDate periodStart,
            @NotNull LocalDate periodEnd,
            String department,
            Set<Long> practitionerUserIds,
            int consultationsCount,
            long totalAmountCents,
            Integer platformFeeRatePercent) {
    }
}
