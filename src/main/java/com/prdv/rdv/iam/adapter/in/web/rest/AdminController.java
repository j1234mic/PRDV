package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.AdminDtos;
import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.AnalyticsQueryUseCase;
import com.prdv.rdv.iam.application.port.input.ApplicationReviewUseCase;
import com.prdv.rdv.iam.application.port.input.AuditQueryUseCase;
import com.prdv.rdv.iam.application.port.input.RoleAdministrationUseCase;
import com.prdv.rdv.iam.application.port.input.UserAdministrationUseCase;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.model.user.AdminType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Console d'administration : validation manuelle, RBAC, comptes, 5 sous-profils
 * d'administrateurs systeme, audit, analytics anonymisees. Chaque endpoint est
 * garde par une permission fine.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration", description = "Validation dossiers, RBAC, administrateurs systeme, audit, KYC, analytics")
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
    public List<Views.PermissionView> permissions(
            @RequestParam(required = false) String module) {
        if (module != null && !module.isBlank()) {
            return roleUseCase.listPermissionsByModule(module);
        }
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

    @PutMapping("/roles/{name}")
    @PreAuthorize("hasAuthority('iam.role.write')")
    @Operation(summary = "Modifier un role personnalise")
    public Views.RoleView updateRole(@PathVariable String name,
                                     @Valid @RequestBody AdminDtos.UpdateRoleRequest request) {
        return roleUseCase.updateRole(new AdminCommands.UpdateCustomRole(
                name, request.description(), request.permissionCodes()));
    }

    @DeleteMapping("/roles/{name}")
    @PreAuthorize("hasAuthority('iam.role.write')")
    @Operation(summary = "Supprimer un role personnalise")
    public ResponseEntity<Void> deleteRole(@PathVariable String name) {
        roleUseCase.deleteCustomRole(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('iam.role.write')")
    public void assignRoles(@PathVariable Long userId,
                            @Valid @RequestBody AdminDtos.AssignRolesRequest request) {
        roleUseCase.assignRoles(new AdminCommands.AssignRoles(userId, request.roleNames()));
    }

    @PutMapping("/users/{userId}/permissions")
    @PreAuthorize("hasAuthority('iam.role.write')")
    @Operation(summary = "Modifier les permissions directes granulaires d'un utilisateur")
    public Views.UserView updateDirectPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody AdminDtos.UpdateDirectPermissionsRequest request) {
        return userUseCase.updateDirectPermissions(
                new AdminCommands.UpdateDirectPermissions(userId, request.directPermissions()));
    }

    // ----------------------------- Administrateurs systeme (5 sous-profils)
    @PostMapping("/administrators")
    @PreAuthorize("hasAuthority('iam.role.write')")
    @Operation(summary = "Creer un administrateur systeme (SUPER_ADMIN, MODERATOR, TECHNICAL_SUPPORT, COMMERCIAL_SUPPORT, DATA_ANALYST)")
    public ResponseEntity<Views.UserView> createAdministrator(
            @Valid @RequestBody AdminDtos.CreateAdministratorRequest request) {
        Views.UserView view = userUseCase.createAdministrator(new AdminCommands.CreateAdministrator(
                request.email(), request.phone(), request.password(), request.adminType()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/administrators")
    @PreAuthorize("hasAuthority('iam.user.read')")
    @Operation(summary = "Lister les administrateurs systeme (filtrable par AdminType)")
    public List<Views.UserView> listAdministrators(
            @RequestParam(required = false) AdminType adminType) {
        return userUseCase.listAdministrators(adminType);
    }

    // ---------------------------------------------------------------- Comptes
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('iam.user.read')")
    public Views.PagedResult<Views.UserView> users(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return userUseCase.listUsers(page, size);
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('iam.user.read')")
    public Views.UserView getUser(@PathVariable Long userId) {
        return userUseCase.getUser(userId);
    }

    @PostMapping("/users/{userId}/status")
    @PreAuthorize("hasAuthority('iam.user.update')")
    public Views.UserView changeStatus(@PathVariable Long userId,
                                       @Valid @RequestBody AdminDtos.ChangeStatusRequest request) {
        return userUseCase.changeAccountStatus(
                new AdminCommands.ChangeAccountStatus(userId, request.suspend()));
    }

    @PostMapping("/users/{userId}/unlock")
    @PreAuthorize("hasAuthority('iam.user.update')")
    @Operation(summary = "Debloquer un compte verrouille apres tentatives echouees")
    public Views.UserView unlockAccount(@PathVariable Long userId) {
        return userUseCase.unlockAccount(userId);
    }

    @PostMapping("/users/{userId}/revoke-tokens")
    @PreAuthorize("hasAuthority('iam.user.update')")
    @Operation(summary = "Revocation instantanee de tous les jetons JWT et sessions d'un utilisateur")
    public Views.UserView revokeUserTokens(@PathVariable Long userId) {
        return userUseCase.revokeUserTokens(userId);
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

    @GetMapping("/analytics/summary")
    @PreAuthorize("hasAuthority('iam.analytics.read')")
    @Operation(summary = "Synthese statistique anonymisee des comptes par profil et statut")
    public Views.AnalyticsSummary analyticsSummary() {
        return userUseCase.anonymizedAnalytics();
    }
}
