package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;

/**
 * Commandes du cas d'usage authentification (CQS : commande = action avec effet).
 */
public final class AuthCommands {

    private AuthCommands() {
    }

    public record Login(String email, String password, RequestMetadata metadata) {
    }

    public record VerifyTotp(String challengeToken, String code, RequestMetadata metadata) {
    }

    /** Finalisation de connexion apres qu'un OTP a ete exige (nouvel appareil, fraude). */
    public record VerifyLoginOtp(String challengeToken, String code, RequestMetadata metadata) {
    }

    public record Refresh(String refreshToken, RequestMetadata metadata) {
    }

    public record Logout(String refreshToken, RequestMetadata metadata) {
    }

    public record VerifyRegistrationOtp(String target, String code, OtpChallenge.Channel channel) {
    }

    public record ResendOtp(String target, OtpChallenge.Channel channel, OtpChallenge.Purpose purpose) {
    }
}
