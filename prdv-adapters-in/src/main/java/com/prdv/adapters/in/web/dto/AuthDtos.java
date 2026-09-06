package com.prdv.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Regroupement des DTO du module auth (records immuables = pattern DTO). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            String phone,
            @NotBlank @Size(min = 10, message = "10 caracteres minimum") String password,
            @NotBlank String role) {
    }

    public record VerifyOtpRequest(@NotBlank @Email String email, @NotBlank String code) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record UserResponse(Long id, String email, String role, String status) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                long accessExpiresInSeconds, UserResponse user) {
    }
}
