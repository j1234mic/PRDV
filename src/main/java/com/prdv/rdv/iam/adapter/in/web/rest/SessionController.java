package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.application.port.input.SessionUseCase;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Sessions", description = "Appareils connectes, historique, revocation")
public class SessionController {

    private final SessionUseCase sessionUseCase;

    public SessionController(SessionUseCase sessionUseCase) {
        this.sessionUseCase = sessionUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('iam.session.read')")
    @Operation(summary = "Historique des sessions (IP, terminal, geolocalisation)")
    public List<Views.SessionView> list() {
        return sessionUseCase.listMySessions();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('iam.session.revoke')")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        sessionUseCase.revoke(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('iam.session.revoke')")
    @Operation(summary = "Deconnecter tous les appareils (revoque aussi les JWT)")
    public ResponseEntity<Void> revokeAll() {
        sessionUseCase.revokeAll();
        return ResponseEntity.noContent().build();
    }
}
