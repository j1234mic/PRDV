package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.AdminDtos;
import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.AnalyticsQueryUseCase;
import com.prdv.rdv.iam.application.port.input.ApplicationReviewUseCase;
import com.prdv.rdv.iam.application.port.input.AuditQueryUseCase;
import com.prdv.rdv.iam.application.port.input.RoleAdministrationUseCase;
import com.prdv.rdv.iam.application.port.input.UserAdministrationUseCase;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Console d'adriculture : validation manuelle, RBAC, comptes, audit,
 * analytics anonymisees. Chaque endpoint est garde par une permission fine.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration", description = "Validation dossiers, RBAC, audit, KYC, analytics")
public class AdminController {

    private final ApplicationReviewUseCase reviewUseCase;
    private final RoleAdministrationUseCase roleUseCase;
    private final UserAdministrationUseCase userUseCase;
    private final AuditQueryUseCase auditUseCase;
    private final AnalyticsQueryUseCase analyticsUseCase;

    public AdminController(ApplicationReviewUseCase reviewUseCase, RoleAdministrationUseCase roleUseCase,
                           UserAdministrationUseCase userUseCase, AuditQueryUseCase auditUseCase,
                           AnalyticsQueryUseCase analyticsUseCase) {
        this.reviewUseCase = reviewUseCase;
        this.roleUseCase = roleUseCase;
        this.userUseCase = userUseCase;
        this.auditUseCase = auditUseCase;
        this.analyticsUseCase = analyticsUseCase;
    }

    // ----------------------------------------------- Validation des dossiers
    @GetMapping("/practitioners/pending")
    @PreAuthorize("hasAuthority('iam.practitioner.read')")
    public List<Views.PractitionerView> pendingPractitioners() {
        return reviewUseCase.pendingPractitioners();
    }

    @GetMapping("/establishments/pending")
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    public List<Views.EstablishmentView> pendingEstablishments() {
        return reviewUseCase.pendingEstablishments();
    }

    @PostMapping("/applications/{userId}/review")
    @PreAuthorize("hasAuthority('iam.practitioner.approve')")
    public Views.UserView reviewApplication(@PathVariable Long userId,
                                            @Valid @RequestBody AdminDtos.ReviewApplicationRequest request) {
        return reviewUseCase.reviewApplication(
                new AdminCommands.ReviewApplication(userId, request.approved(), request.reason()));
    }

    @GetMapping("/documents/pending")
    @PreAuthorize("hasAuthority('iam.kyc.read')")
    public Views.PagedResult<Views.KycDocumentView> pendingDocuments(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return reviewUseCase.pendingDocuments(page, size);
    }

    @PostMapping("/documents/{id}/review")
    @PreAuthorize("hasAuthority('iam.kyc.review')")
    public Views.KycDocumentView reviewDocument(@PathVariable Long id,
                                                @Valid @RequestBody AdminDtos.ReviewDocumentRequest request) {
        return reviewUseCase.reviewDocument(
                new AdminCommands.ReviewDocument(id, request.approved(), request.note()));
    }

    // ------------------------------------------------------------------ RBAC
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('iam.role.read')")
    public List<Views.PermissionView> permissions() {
        return roleUseCase.listPermissions();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('iam.role.read')")
    public List<Views.RoleView> roles() {
        return roleUseCase.listRoles();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('iam.role.write')")
    public Views.RoleView createRole(@Valid @RequestBody AdminDtos.CreateRoleRequest request) {
        return roleUseCase.createRole(new AdminCommands.CreateRole(
                request.name(), request.description(), request.permissionCodes()));
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('iam.role.write')")
    public void assignRoles(@PathVariable Long userId,
                            @Valid @RequestBody AdminDtos.AssignRolesRequest request) {
        roleUseCase.assignRoles(new AdminCommands.AssignRoles(userId, request.roleNames()));
    }

    // ---------------------------------------------------------------- Comptes
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('iam.user.read')")
    public Views.PagedResult<Views.UserView> users(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return userUseCase.listUsers(page, size);
    }

    @PostMapping("/users/{userId}/status")
    @PreAuthorize("hasAuthority('iam.user.update')")
    public Views.UserView changeStatus(@PathVariable Long userId,
                                       @Valid @RequestBody AdminDtos.ChangeStatusRequest request) {
        return userUseCase.changeAccountStatus(
                new AdminCommands.ChangeAccountStatus(userId, request.suspend()));
    }

    // ----------------------------------------------------------------- Audit
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('iam.audit.read')")
    public Views.PagedResult<Views.AuditView> audit(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        return auditUseCase.listAll(page, size);
    }

    @GetMapping("/users/{userId}/audit")
    @PreAuthorize("hasAuthority('iam.audit.read')")
    public Views.PagedResult<Views.AuditView> userAudit(@PathVariable Long userId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return auditUseCase.listByUser(userId, page, size);
    }

    // ------------------------------------------------------------- Analytics
    @GetMapping("/analytics/anonymized-users")
    @PreAuthorize("hasAuthority('iam.analytics.read')")
    public List<Views.AnonymizedUserView> anonymizedUsers() {
        return analyticsUseCase.anonymizedUsers();
    }
}
