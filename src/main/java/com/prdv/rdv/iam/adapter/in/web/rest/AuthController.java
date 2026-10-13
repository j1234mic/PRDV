package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.HttpRequestMetadata;
import com.prdv.rdv.iam.adapter.in.web.dto.AuthDtos;
import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.AuthenticationUseCase;
import com.prdv.rdv.iam.application.port.input.BiometricAuthenticationUseCase;
import com.prdv.rdv.iam.application.port.input.ContactVerificationUseCase;
import com.prdv.rdv.iam.application.port.input.MfaManagementUseCase;
import com.prdv.rdv.iam.application.port.input.PatientRegistrationUseCase;
import com.prdv.rdv.iam.application.port.input.SocialAuthenticationUseCase;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints publics d'authentification, biometrie, SSO entreprise et gestion du second facteur.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, OTP, MFA, biometrie, SSO, sessions")
public class AuthController {

    private final AuthenticationUseCase authenticationUseCase;
    private final ContactVerificationUseCase contactVerificationUseCase;
    private final PatientRegistrationUseCase patientRegistrationUseCase;
    private final MfaManagementUseCase mfaManagementUseCase;
    private final BiometricAuthenticationUseCase biometricAuthenticationUseCase;
    private final SocialAuthenticationUseCase socialAuthenticationUseCase;
    private final HttpRequestMetadata httpRequestMetadata;

    public AuthController(AuthenticationUseCase authenticationUseCase,
                          ContactVerificationUseCase contactVerificationUseCase,
                          PatientRegistrationUseCase patientRegistrationUseCase,
                          MfaManagementUseCase mfaManagementUseCase,
                          HttpRequestMetadata httpRequestMetadata) {
        this(authenticationUseCase, contactVerificationUseCase, patientRegistrationUseCase,
                mfaManagementUseCase, null, null, httpRequestMetadata);
    }

    @Autowired
    public AuthController(AuthenticationUseCase authenticationUseCase,
                          ContactVerificationUseCase contactVerificationUseCase,
                          PatientRegistrationUseCase patientRegistrationUseCase,
                          MfaManagementUseCase mfaManagementUseCase,
                          BiometricAuthenticationUseCase biometricAuthenticationUseCase,
                          SocialAuthenticationUseCase socialAuthenticationUseCase,
                          HttpRequestMetadata httpRequestMetadata) {
        this.authenticationUseCase = authenticationUseCase;
        this.contactVerificationUseCase = contactVerificationUseCase;
        this.patientRegistrationUseCase = patientRegistrationUseCase;
        this.mfaManagementUseCase = mfaManagementUseCase;
        this.biometricAuthenticationUseCase = biometricAuthenticationUseCase;
        this.socialAuthenticationUseCase = socialAuthenticationUseCase;
        this.httpRequestMetadata = httpRequestMetadata;
    }

    @PostMapping("/register/patient")
    @Operation(summary = "Inscription d'un patient par email ou telephone (declenche un OTP email/SMS)")
    public Views.OtpSentView registerPatient(@Valid @RequestBody AuthDtos.RegisterPatientRequest request,
                                             HttpServletRequest httpRequest) {
        return patientRegistrationUseCase.register(new RegistrationCommands.RegisterPatient(
                request.email(), request.phone(), request.password(), request.firstName(),
                request.lastName(), request.birthDate(), request.gender(), request.guardianUserId(),
                httpRequestMetadata.from(httpRequest)));
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion mot de passe (retourne les jetons ou un defi MFA/OTP)")
    public AuthResults.AuthResult login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                        HttpServletRequest httpRequest) {
        return authenticationUseCase.login(new AuthCommands.Login(
                request.email(), request.password(), httpRequestMetadata.from(httpRequest)));
    }

    @PostMapping("/login/totp")
    @Operation(summary = "Validation du code TOTP apres un defi MFA")
    public AuthResults.AuthResult verifyTotp(@Valid @RequestBody AuthDtos.TotpLoginRequest request,
                                             HttpServletRequest httpRequest) {
        return authenticationUseCase.verifyTotp(new AuthCommands.VerifyTotp(
                request.challengeToken(), request.code(), httpRequestMetadata.from(httpRequest)));
    }

    @PostMapping("/login/otp")
    @Operation(summary = "Validation du code OTP envoye par email (connexion a risque)")
    public AuthResults.AuthResult verifyLoginOtp(@Valid @RequestBody AuthDtos.OtpLoginRequest request,
                                                 HttpServletRequest httpRequest) {
        return authenticationUseCase.verifyLoginOtp(new AuthCommands.VerifyLoginOtp(
                request.challengeToken(), request.code(), httpRequestMetadata.from(httpRequest)));
    }

    // ----------------------------------- Biometrie (Empreinte, Face ID, Passkey)
    @PostMapping("/biometric/enroll")
    @PreAuthorize("hasAuthority('iam.biometric.manage')")
    @Operation(summary = "Enregistrer un facteur biometrique (Touch ID / Face ID / Passkey)")
    public ResponseEntity<Views.BiometricCredentialView> enrollBiometric(
            @Valid @RequestBody AuthDtos.EnrollBiometricRequest request) {
        Views.BiometricCredentialView view = biometricAuthenticationUseCase.enroll(
                new AuthCommands.EnrollBiometric(
                        request.credentialId(), request.publicKey(),
                        request.modality(), request.deviceName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/biometric/credentials")
    @PreAuthorize("hasAuthority('iam.biometric.manage')")
    @Operation(summary = "Lister ses facteurs biometriques enregistres")
    public List<Views.BiometricCredentialView> listBiometricCredentials() {
        return biometricAuthenticationUseCase.listMyCredentials();
    }

    @DeleteMapping("/biometric/credentials/{id}")
    @PreAuthorize("hasAuthority('iam.biometric.manage')")
    @Operation(summary = "Revoquer un facteur biometrique")
    public ResponseEntity<Void> revokeBiometricCredential(@PathVariable Long id) {
        biometricAuthenticationUseCase.revoke(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/biometric/challenge")
    @Operation(summary = "Obtenir un defi cryptographique pour une connexion biometrique")
    public AuthResults.Challenge requestBiometricChallenge(
            @Valid @RequestBody AuthDtos.BiometricChallengeRequest request) {
        return biometricAuthenticationUseCase.issueChallenge(
                new AuthCommands.RequestBiometricChallenge(request.email(), request.credentialId()));
    }

    @PostMapping("/biometric/verify")
    @Operation(summary = "Authentification par assertion biometrique signee")
    public AuthResults.AuthResult verifyBiometric(
            @Valid @RequestBody AuthDtos.VerifyBiometricRequest request,
            HttpServletRequest httpRequest) {
        return biometricAuthenticationUseCase.authenticate(new AuthCommands.VerifyBiometric(
                request.challengeToken(), request.credentialId(),
                request.signature(), request.signCount(),
                httpRequestMetadata.from(httpRequest)));
    }

    // --------------------------------------------------- SSO Entreprise
    @PostMapping("/sso/enterprise")
    @Operation(summary = "Connexion Single Sign-On (SSO) entreprise (Hopital / Clinique / Federation)")
    public AuthResults.AuthResult enterpriseSsoLogin(
            @Valid @RequestBody AuthDtos.EnterpriseSsoLoginRequest request,
            HttpServletRequest httpRequest) {
        return AuthResults.AuthResult.authenticated(
                socialAuthenticationUseCase.authenticateEnterpriseSso(
                        new AuthCommands.EnterpriseSsoLogin(
                                request.tenantDomain(), request.externalSubject(),
                                request.email(), request.firstName(), request.lastName(),
                                request.targetProfileType(),
                                httpRequestMetadata.from(httpRequest))));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotation du refresh token")
    public AuthResults.TokenSet refresh(@Valid @RequestBody AuthDtos.RefreshRequest request,
                                        HttpServletRequest httpRequest) {
        return authenticationUseCase.refresh(new AuthCommands.Refresh(
                request.refreshToken(), httpRequestMetadata.from(httpRequest)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Deconnexion (revoque le refresh token et la session)")
    public ResponseEntity<Void> logout(@RequestBody(required = false) AuthDtos.LogoutRequest request,
                                       HttpServletRequest httpRequest) {
        if (request != null && request.refreshToken() != null) {
            authenticationUseCase.logout(new AuthCommands.Logout(
                    request.refreshToken(), httpRequestMetadata.from(httpRequest)));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Confirmation du code OTP lors de l'inscription")
    public Views.UserView verifyRegistrationOtp(@Valid @RequestBody AuthDtos.VerifyOtpRequest request) {
        return contactVerificationUseCase.confirmRegistrationOtp(
                new AuthCommands.VerifyRegistrationOtp(request.target(), request.code(), request.channel()));
    }

    @PostMapping("/otp/resend")
    @Operation(summary = "Renvoyer un code OTP (anti-abus : 60 s)")
    public Views.OtpSentView resendOtp(@Valid @RequestBody AuthDtos.ResendOtpRequest request) {
        return contactVerificationUseCase.resend(
                new AuthCommands.ResendOtp(request.target(), request.channel(), request.purpose()));
    }

    // --------------------------------------------------------------- MFA
    @PostMapping("/mfa/setup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Preparer la MFA TOTP (secret + URI otpauth pour QR code)")
    public AuthResults.MfaSetup setupMfa() {
        return mfaManagementUseCase.beginSetup();
    }

    @PostMapping("/mfa/enable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Activer la MFA apres verification du premier code")
    public ResponseEntity<Void> enableMfa(@Valid @RequestBody AuthDtos.MfaEnableRequest request) {
        mfaManagementUseCase.enable(request.code());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/mfa")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Desactiver la MFA")
    public ResponseEntity<Void> disableMfa() {
        mfaManagementUseCase.disable();
        return ResponseEntity.noContent().build();
    }
}
