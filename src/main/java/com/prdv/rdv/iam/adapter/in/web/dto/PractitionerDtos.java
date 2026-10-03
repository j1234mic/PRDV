package com.prdv.rdv.iam.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Requetes liees aux praticiens, contrats et rattachements.
 */
public final class PractitionerDtos {

    private static final DateTimeFormatter FRENCH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
            @NotNull(message = "L'identifiant de l'etablissement (establishmentUserId) est obligatoire")
            @JsonAlias({"establishmentId", "cabinetId", "establishment_user_id", "establishment_id"})
            Long establishmentUserId,
            @JsonAlias({"memberRole", "member_role"})
            EstablishmentMembership.MemberRole role,
            @JsonDeserialize(using = LenientLocalDateDeserializer.class)
            LocalDate validFrom,
            @JsonDeserialize(using = LenientLocalDateDeserializer.class)
            LocalDate validUntil) {

        public MembershipRequest {
            if (role == null) {
                role = EstablishmentMembership.MemberRole.EMPLOYEE;
            }
        }
    }

    public record ReplacementRequest(
            @NotNull(message = "L'identifiant de l'etablissement (establishmentUserId) est obligatoire")
            @JsonAlias({"establishmentId", "cabinetId", "establishment_user_id", "establishment_id"})
            Long establishmentUserId,
            @JsonDeserialize(using = LenientLocalDateDeserializer.class)
            LocalDate validFrom,
            @JsonDeserialize(using = LenientLocalDateDeserializer.class)
            LocalDate validUntil) {
    }

    /**
     * Deserialiseur tolerant pour les dates de validite : accepte {@code YYYY-MM-DD},
     * les horodatages ISO-8601 ({@code 2026-09-12T08:00:00Z} / {@code 2026-09-12T08:00:00}),
     * le format francais {@code DD/MM/YYYY} ainsi que les chaines vides ({@code null}).
     */
    public static final class LenientLocalDateDeserializer extends JsonDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parseLocalDate(parser.getValueAsString());
        }
    }

    public static LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // autres formats supportes ci-dessous
        }
        if (trimmed.length() >= 10 && (trimmed.indexOf('T') == 10 || trimmed.indexOf(' ') == 10)) {
            try {
                return LocalDate.parse(trimmed.substring(0, 10));
            } catch (DateTimeParseException ignored) {
                // tentative horodatage complet ci-dessous
            }
        }
        try {
            return OffsetDateTime.parse(trimmed).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return Instant.parse(trimmed).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(trimmed).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(trimmed, FRENCH_DATE);
        } catch (DateTimeParseException ignored) {
        }
        throw new IllegalArgumentException(
                "Format de date invalide : \"" + raw + "\" (attendu : YYYY-MM-DD ou DD/MM/YYYY)");
    }
}
