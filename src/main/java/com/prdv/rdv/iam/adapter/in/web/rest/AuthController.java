package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.HttpRequestMetadata;
import com.prdv.rdv.iam.adapter.in.web.dto.AuthDtos;
import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.AuthenticationUseCase;
import com.prdv.rdv.iam.application.port.input.ContactVerificationUseCase;
import com.prdv.rdv.iam.application.port.input.MfaManagementUseCase;
import com.prdv.rdv.iam.application.port.input.PatientRegistrationUseCase;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publics d'authentification et de gestion du second facteur.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, OTP, MFA, sessions")
public class AuthController {

    private final AuthenticationUseCase authenticationUseCase;
    private final ContactVerificationUseCase contactVerificationUseCase;
    private final PatientRegistrationUseCase patientRegistrationUseCase;
    private final MfaManagementUseCase mfaManagementUseCase;
    private final HttpRequestMetadata httpRequestMetadata;

    public AuthController(AuthenticationUseCase authenticationUseCase,
                          ContactVerificationUseCase contactVerificationUseCase,
                          PatientRegistrationUseCase patientRegistrationUseCase,
                          MfaManagementUseCase mfaManagementUseCase,
                          HttpRequestMetadata httpRequestMetadata) {
        this.authenticationUseCase = authenticationUseCase;
        this.contactVerificationUseCase = contactVerificationUseCase;
        this.patientRegistrationUseCase = patientRegistrationUseCase;
        this.mfaManagementUseCase = mfaManagementUseCase;
        this.httpRequestMetadata = httpRequestMetadata;
    }

    @PostMapping("/register/patient")
    @Operation(summary = "Inscription d'un patient (declenche un OTP email/SMS)")
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
