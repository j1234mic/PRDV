package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.PractitionerDtos;
import com.prdv.rdv.iam.adapter.in.web.dto.SecretaryDtos;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.PractitionerRegistrationUseCase;
import com.prdv.rdv.iam.application.port.input.SecretaryRegistrationUseCase;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/practitioners")
@Tag(name = "Praticiens", description = "Inscription, KYC, contrats, multi-etablissements, remplaçants")
public class PractitionerController {

    private final PractitionerRegistrationUseCase practitionerUseCase;
    private final SecretaryRegistrationUseCase secretaryUseCase;
    private final com.prdv.rdv.iam.application.port.output.SecurityContextPort securityContext;

    public PractitionerController(PractitionerRegistrationUseCase practitionerUseCase,
                                  SecretaryRegistrationUseCase secretaryUseCase,
                                  com.prdv.rdv.iam.application.port.output.SecurityContextPort securityContext) {
        this.practitionerUseCase = practitionerUseCase;
        this.secretaryUseCase = secretaryUseCase;
        this.securityContext = securityContext;
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription praticien (verification RPPS/ADELI, RIB, en attente de validation)")
    public Views.OtpSentView register(@Valid @RequestBody PractitionerDtos.RegisterPractitionerRequest request) {
        return practitionerUseCase.register(new RegistrationCommands.RegisterPractitioner(
                request.email(), request.phone(), request.password(), request.firstName(),
                request.lastName(), request.specialty(), request.rppsNumber(), request.adeliNumber(),
                request.iban(), null));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('iam.practitioner.read')")
    public Views.PractitionerView me() {
        return practitionerUseCase.currentProfile();
    }

    @GetMapping("/me/documents")
    @PreAuthorize("hasAuthority('iam.kyc.read')")
    public List<Views.KycDocumentView> myDocuments() {
        return practitionerUseCase.myDocuments();
    }

    @PostMapping(path = "/me/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('iam.kyc.upload')")
    @Operation(summary = "Deposer diplome, RIB, attestation RC professionnelle ou contrat signe")
    public Views.KycDocumentView uploadDocument(@RequestParam("file") MultipartFile file,
                                                @RequestParam("type") KycDocument.DocumentType type) {
        return practitionerUseCase.uploadDocument(new ProfileCommands.UploadKycDocument(
                null, type, file.getOriginalFilename(), file.getContentType(), readBytes(file)));
    }

    @PostMapping("/me/contract")
    @PreAuthorize("hasAuthority('iam.practitioner.read')")
    @Operation(summary = "Accepter le contrat electronique d'adhesion")
    public Views.ContractView acceptContract(@Valid @RequestBody PractitionerDtos.AcceptContractRequest request,
                                             HttpServletRequest httpRequest) {
        return practitionerUseCase.acceptContract(new ProfileCommands.AcceptContract(
                request.version(), request.contentHash(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/me/memberships")
    @PreAuthorize("hasAuthority('iam.practitioner.read')")
    @Operation(summary = "Demander un rattachement a un cabinet (multi-etablissements)")
    public Views.MembershipView requestMembership(@Valid @RequestBody PractitionerDtos.MembershipRequest request) {
        return practitionerUseCase.requestMembership(new ProfileCommands.RequestMembership(
                request.establishmentUserId(), request.role(), request.validFrom(), request.validUntil()));
    }

    @PostMapping("/me/replacements")
    @PreAuthorize("hasAuthority('iam.practitioner.read')")
    @Operation(summary = "Declarer un remplacement dans un etablissement sur une plage donnee")
    public Views.MembershipView declareReplacement(@Valid @RequestBody PractitionerDtos.ReplacementRequest request) {
        return practitionerUseCase.declareReplacement(new ProfileCommands.DeclareReplacement(
                request.establishmentUserId(), request.validFrom(), request.validUntil()));
    }

    @GetMapping("/me/memberships")
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    public List<Views.MembershipView> myMemberships() {
        return practitionerUseCase.myMemberships();
    }

    @GetMapping("/me/secretaries")
    @PreAuthorize("hasAuthority('iam.practitioner.read')")
    @Operation(summary = "Secretaires rattachees au praticien connecte")
    public List<Views.SecretaryView> mySecretaries() {
        return secretaryUseCase.listForPractitioner(securityContext.requireCurrentUserId());
    }

    @PostMapping("/secretaries")
    @PreAuthorize("hasAuthority('iam.secretary.register')")
    @Operation(summary = "Creer un compte secretaire medicale rattache")
    public Views.SecretaryView createSecretary(@Valid @RequestBody SecretaryDtos.RegisterSecretaryRequest request) {
        return secretaryUseCase.create(new RegistrationCommands.RegisterSecretary(
                request.email(), request.firstName(), request.lastName(),
                request.supervisedPractitionerIds(), request.permissionCodes()));
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Fichier illisible");
        }
    }
}
