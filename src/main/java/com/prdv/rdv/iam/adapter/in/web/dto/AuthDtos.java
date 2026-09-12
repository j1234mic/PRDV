package com.prdv.rdv.iam.adapter.in.web.dto;

import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Requetes des endpoints d'authentification et d'inscription patient.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterPatientRequest(
            @NotBlank @Email String email,
            @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "telephone E.164 ex : +33612345678") String phone,
            @NotBlank @Size(min = 8, message = "au moins 8 caracteres") String password,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Past LocalDate birthDate,
            PatientProfile.Gender gender,
            Long guardianUserId) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record TotpLoginRequest(@NotBlank String challengeToken, @NotBlank String code) {
    }

    public record OtpLoginRequest(@NotBlank String challengeToken, @NotBlank String code) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record VerifyOtpRequest(@NotBlank String target, @NotBlank String code,
                                   @NotNull OtpChallenge.Channel channel) {
    }

    public record ResendOtpRequest(@NotBlank String target, @NotNull OtpChallenge.Channel channel,
                                   @NotNull OtpChallenge.Purpose purpose) {
    }

    public record MfaEnableRequest(@NotBlank String code) {
    }
}
