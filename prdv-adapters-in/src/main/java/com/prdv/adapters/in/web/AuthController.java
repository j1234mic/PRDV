package com.prdv.adapters.in.web;

import com.prdv.adapters.in.web.dto.AuthDtos.LoginRequest;
import com.prdv.adapters.in.web.dto.AuthDtos.RefreshRequest;
import com.prdv.adapters.in.web.dto.AuthDtos.RegisterRequest;
import com.prdv.adapters.in.web.dto.AuthDtos.TokenResponse;
import com.prdv.adapters.in.web.dto.AuthDtos.UserResponse;
import com.prdv.adapters.in.web.dto.AuthDtos.VerifyOtpRequest;
import com.prdv.identity.application.model.AuthTokens;
import com.prdv.identity.application.port.in.LoginUseCase;
import com.prdv.identity.application.port.in.RefreshTokenUseCase;
import com.prdv.identity.application.port.in.RegisterUserUseCase;
import com.prdv.identity.application.port.in.VerifyEmailUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur PRIMAIRE : traduction HTTP -> ports d'entree.
 * Aucune logique ici (SRP) : le controleur valide l'enveloppe et delègue.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase register;
    private final VerifyEmailUseCase verifyEmail;
    private final LoginUseCase login;
    private final RefreshTokenUseCase refresh;

    public AuthController(RegisterUserUseCase register, VerifyEmailUseCase verifyEmail,
                          LoginUseCase login, RefreshTokenUseCase refresh) {
        this.register = register;
        this.verifyEmail = verifyEmail;
        this.login = login;
        this.refresh = refresh;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var info = register.register(new RegisterUserUseCase.Command(
                request.email(), request.phone(), request.password(), request.role(), null));
        return ResponseEntity.status(HttpStatus.CREATED).body(DtoMapper.toResponse(info));
    }

    @PostMapping("/verify-email")
    public UserResponse verify(@Valid @RequestBody VerifyOtpRequest request) {
        return DtoMapper.toResponse(verifyEmail.verify(request.email(), request.code()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = login.login(new LoginUseCase.Command(request.email(), request.password()));
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(),
                tokens.accessExpiresInSeconds(), null);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        AuthTokens tokens = refresh.refresh(request.refreshToken());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(),
                tokens.accessExpiresInSeconds(), null);
    }
}
