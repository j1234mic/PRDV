package com.prdv.rdv.iam.adapter.in.web.dto;

import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;

public final class PatientDtos {

    private PatientDtos() {
    }

    /** Import depuis une autre plateforme (anti-corruption : format csv/json...). */
    public record ImportRequest(@NotBlank String format, @NotBlank String content, String sourcePlatform) {
    }

    /** Creation d'un profil mineur rattache au compte parent / tuteur connecte. */
    public record RegisterMinorRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Past LocalDate birthDate,
            PatientProfile.Gender gender) {
    }

    /** Etat du dossier KYC et d'eligibilite aux paiements du patient. */
    public record KycStatusResponse(
            Long userId,
            PatientProfile.KycLevel kycLevel,
            boolean identityDocumentVerified,
            boolean paymentEligible,
            List<Views.KycDocumentView> documents) {
    }
}
