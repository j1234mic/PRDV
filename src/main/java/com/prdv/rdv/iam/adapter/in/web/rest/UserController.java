package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.application.port.input.AccountLifecycleUseCase;
import com.prdv.rdv.iam.application.port.input.UserQueryUseCase;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Compte", description = "Consultation et droit a l'oubli")
public class UserController {

    private final UserQueryUseCase userQueryUseCase;
    private final AccountLifecycleUseCase accountLifecycleUseCase;

    public UserController(UserQueryUseCase userQueryUseCase,
                          AccountLifecycleUseCase accountLifecycleUseCase) {
        this.userQueryUseCase = userQueryUseCase;
        this.accountLifecycleUseCase = accountLifecycleUseCase;
    }

    @GetMapping
    @Operation(summary = "Profil de l'utilisateur connecte")
    public Views.UserView me() {
        return userQueryUseCase.currentUser();
    }

    @DeleteMapping
    @Operation(summary = "Supprimer/anonymiser mon compte (RGPD)")
    public ResponseEntity<Void> deleteMyAccount() {
        accountLifecycleUseCase.deleteMyAccount();
        return ResponseEntity.noContent().build();
    }
}
