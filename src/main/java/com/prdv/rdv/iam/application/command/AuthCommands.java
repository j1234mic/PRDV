package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.auth.BiometricCredential;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import com.prdv.rdv.iam.domain.model.user.ProfileType;

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

    /** Enrolement d'un facteur biometrique (empreinte digitale, Face ID, Passkey WebAuthn). */
    public record EnrollBiometric(String credentialId,
                                  String publicKey,
                                  BiometricCredential.BiometricModality modality,
                                  String deviceName) {
    }

    /** Demande de defi (challenge) pour une connexion biometrique. */
    public record RequestBiometricChallenge(String email, String credentialId) {
    }

    /** Connexion par verification de signature biometrique (WebAuthn / FIDO2 assertion). */
    public record VerifyBiometric(String challengeToken,
                                  String credentialId,
                                  String signature,
                                  long signCount,
                                  RequestMetadata metadata) {
    }

    /** Connexion / inscription via OAuth 2.0 / OpenID Connect (reseaux sociaux). */
    public record SocialLogin(SocialAccount.Provider provider,
                              String providerUserId,
                              String email,
                              String firstName,
                              String lastName,
                              RequestMetadata metadata) {
    }

    /** Connexion via SSO (Single Sign-On) d'entreprise (hopital, clinique, groupe de sante). */
    public record EnterpriseSsoLogin(String enterpriseDomain,
                                     String idpSubject,
                                     String email,
                                     String firstName,
                                     String lastName,
                                     ProfileType profileType,
                                     RequestMetadata metadata) {
    }
}
