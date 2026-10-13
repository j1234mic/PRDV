package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.EstablishmentDtos;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.EstablishmentRegistrationUseCase;
import com.prdv.rdv.iam.application.result.Views;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/establishments")
@Tag(name = "Etablissements", description = "Comptes entreprise, departements, rattachements, facturation groupee, dashboard consolide")
public class EstablishmentController {

    private final EstablishmentRegistrationUseCase establishmentUseCase;

    public EstablishmentController(EstablishmentRegistrationUseCase establishmentUseCase) {
        this.establishmentUseCase = establishmentUseCase;
    }

    @PostMapping("/register")
    public Views.OtpSentView register(@Valid @RequestBody EstablishmentDtos.RegisterEstablishmentRequest request) {
        return establishmentUseCase.register(new RegistrationCommands.RegisterEstablishment(
                request.email(), request.phone(), request.password(), request.legalName(),
                request.siret(), request.address(), request.departments(), null));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    public List<Views.EstablishmentView> list() {
        return establishmentUseCase.listEstablishments();
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    public Views.EstablishmentView me() {
        return establishmentUseCase.currentProfile();
    }

    @PutMapping("/me/departments")
    @PreAuthorize("hasAuthority('iam.establishment.manage')")
    @Operation(summary = "Mettre a jour les services / departements de l'etablissement")
    public Views.EstablishmentView updateDepartments(
            @Valid @RequestBody EstablishmentDtos.UpdateDepartmentsRequest request) {
        return establishmentUseCase.updateDepartments(
                new ProfileCommands.UpdateEstablishmentDepartments(request.departments()));
    }

    @GetMapping("/me/memberships")
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    public List<Views.MembershipView> memberships() {
        return establishmentUseCase.memberships();
    }

    @PostMapping("/me/memberships/{id}/decision")
    @PreAuthorize("hasAuthority('iam.establishment.manage')")
    public Views.MembershipView reviewMembership(@PathVariable Long id,
                                                 @Valid @RequestBody EstablishmentDtos.MembershipDecisionRequest request) {
        return establishmentUseCase.reviewMembership(
                new ProfileCommands.ReviewMembership(id, request.approved()));
    }

    @DeleteMapping("/me/memberships/{id}")
    @PreAuthorize("hasAuthority('iam.establishment.manage')")
    @Operation(summary = "Revoquer le rattachement d'un praticien a l'etablissement")
    public Views.MembershipView revokeMembership(@PathVariable Long id) {
        return establishmentUseCase.revokeMembership(id);
    }

    // ------------------------------------------------- Facturation groupee
    @PostMapping("/me/billing/invoices")
    @PreAuthorize("hasAuthority('billing.write')")
    @Operation(summary = "Emettre une facture groupee multi-praticiens pour l'etablissement")
    public ResponseEntity<Views.GroupInvoiceView> issueGroupInvoice(
            @Valid @RequestBody EstablishmentDtos.IssueGroupInvoiceRequest request) {
        Views.GroupInvoiceView invoice = establishmentUseCase.issueGroupInvoice(
                new ProfileCommands.IssueGroupInvoice(
                        request.periodStart(),
                        request.periodEnd(),
                        request.department(),
                        request.practitionerUserIds(),
                        request.consultationsCount(),
                        request.totalAmountCents(),
                        request.platformFeeRatePercent()));
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @GetMapping("/me/billing/invoices")
    @PreAuthorize("hasAuthority('billing.read')")
    @Operation(summary = "Lister les factures groupees de l'etablissement")
    public List<Views.GroupInvoiceView> listGroupInvoices() {
        return establishmentUseCase.listGroupInvoices();
    }

    @PostMapping("/me/billing/invoices/{id}/pay")
    @PreAuthorize("hasAuthority('billing.write')")
    @Operation(summary = "Enregistrer le reglement d'une facture groupee")
    public Views.GroupInvoiceView payGroupInvoice(@PathVariable Long id) {
        return establishmentUseCase.payGroupInvoice(id);
    }

    // ------------------------------------------------- Tableau de bord consolide
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    @Operation(summary = "Tableau de bord consolide (praticiens, remplacants, secretaires, services, facturation)")
    public Views.EstablishmentDashboardView consolidatedDashboard() {
        return establishmentUseCase.consolidatedDashboard();
    }
}
