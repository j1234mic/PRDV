package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.SecretaryDtos;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.port.input.SecretaryRegistrationUseCase;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/secretaries")
@Tag(name = "Secretaires Medicales", description = "Profil, droits granulaires, acces multi-cabinets et historique des actions")
public class SecretaryController {

    private final SecretaryRegistrationUseCase secretaryUseCase;
    private final SecurityContextPort securityContext;

    public SecretaryController(SecretaryRegistrationUseCase secretaryUseCase,
                               SecurityContextPort securityContext) {
        this.secretaryUseCase = secretaryUseCase;
        this.securityContext = securityContext;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('iam.user.read')")
    @Operation(summary = "Consulter son profil secretaire medicale (medecins rattaches et cabinets autorises)")
    public Views.SecretaryDetailView me() {
        return secretaryUseCase.currentProfile();
    }

    @GetMapping("/me/audit")
    @PreAuthorize("hasAuthority('iam.user.read')")
    @Operation(summary = "Consulter son propre historique d'actions (audit trail)")
    public Views.PagedResult<Views.AuditView> myAuditTrail(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = securityContext.requireCurrentUserId();
        return secretaryUseCase.secretaryAuditTrail(userId, page, size);
    }

    @PutMapping("/{userId}/permissions")
    @PreAuthorize("hasAuthority('iam.secretary.register')")
    @Operation(summary = "Modifier les droits granulaires d'une secretaire medicale")
    public Views.SecretaryDetailView updatePermissions(
            @PathVariable Long userId,
            @Valid @RequestBody SecretaryDtos.UpdateSecretaryPermissionsRequest request) {
        return secretaryUseCase.updatePermissions(
                new ProfileCommands.UpdateSecretaryPermissions(userId, request.permissionCodes()));
    }

    @PutMapping("/{userId}/assignments")
    @PreAuthorize("hasAuthority('iam.secretary.register')")
    @Operation(summary = "Modifier les medecins rattaches et les acces multi-cabinets d'une secretaire")
    public Views.SecretaryDetailView updateAssignments(
            @PathVariable Long userId,
            @Valid @RequestBody SecretaryDtos.UpdateSecretaryAssignmentsRequest request) {
        return secretaryUseCase.updateAssignments(
                new ProfileCommands.UpdateSecretaryAssignments(
                        userId,
                        request.supervisedPractitionerIds(),
                        request.accessibleEstablishmentIds()));
    }

    @GetMapping("/{userId}/audit")
    @PreAuthorize("hasAuthority('iam.secretary.register')")
    @Operation(summary = "Consulter l'historique des actions (audit trail) d'une secretaire")
    public Views.PagedResult<Views.AuditView> secretaryAuditTrail(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return secretaryUseCase.secretaryAuditTrail(userId, page, size);
    }
}
