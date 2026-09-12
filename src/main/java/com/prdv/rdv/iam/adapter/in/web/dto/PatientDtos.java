package com.prdv.rdv.iam.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public final class PatientDtos {

    private PatientDtos() {
    }

    /** Import depuis une autre plateforme (anti-corruption : format csv/json...). */
    public record ImportRequest(@NotBlank String format, @NotBlank String content, String sourcePlatform) {
    }
}
