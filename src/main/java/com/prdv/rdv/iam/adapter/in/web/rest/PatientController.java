package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.PatientDtos;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.DataImportUseCase;
import com.prdv.rdv.iam.application.port.input.PatientRegistrationUseCase;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Profil patient, KYC piece d'identite, profils mineurs et import externe")
public class PatientController {

    private final PatientRegistrationUseCase patientUseCase;
    private final DataImportUseCase dataImportUseCase;

    public PatientController(PatientRegistrationUseCase patientUseCase,
                             DataImportUseCase dataImportUseCase) {
        this.patientUseCase = patientUseCase;
        this.dataImportUseCase = dataImportUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('iam.user.read')")
    @Operation(summary = "Consulter son profil patient et son niveau KYC")
    public Views.PatientView me() {
        return patientUseCase.currentProfile();
    }

    @PostMapping(path = "/me/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('iam.kyc.upload')")
    @Operation(summary = "Deposer CNI / passeport pour la verification d'identite KYC")
    public Views.KycDocumentView uploadIdentityDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") KycDocument.DocumentType type) {
        return patientUseCase.uploadIdentityDocument(new ProfileCommands.UploadKycDocument(
                null, type, file.getOriginalFilename(), file.getContentType(), readBytes(file)));
    }

    @GetMapping("/me/documents")
    @PreAuthorize("hasAuthority('iam.kyc.read')")
    @Operation(summary = "Lister ses pieces d'identite KYC deposees")
    public List<Views.KycDocumentView> myDocuments() {
        return patientUseCase.myDocuments();
    }

    @GetMapping("/me/kyc")
    @PreAuthorize("hasAuthority('iam.kyc.read')")
    @Operation(summary = "Consulter l'etat de son dossier KYC et son eligibilite aux paiements")
    public PatientDtos.KycStatusResponse myKycStatus() {
        Views.PatientView profile = patientUseCase.currentProfile();
        List<Views.KycDocumentView> docs = patientUseCase.myDocuments();
        boolean verified = docs.stream().anyMatch(d -> d.status() == KycDocument.Status.VERIFIED);
        boolean paymentEligible = profile.kycLevel() == PatientProfile.KycLevel.IDENTITY_VERIFIED
                || profile.kycLevel() == PatientProfile.KycLevel.PAYMENT_READY;
        return new PatientDtos.KycStatusResponse(
                profile.user().id(),
                profile.kycLevel(),
                verified,
                paymentEligible,
                docs);
    }

    @PostMapping("/me/minors")
    @PreAuthorize("hasAuthority('iam.user.update')")
    @Operation(summary = "Creer un profil mineur rattache au compte parent / tuteur connecte")
    public ResponseEntity<Views.PatientView> registerMinor(
            @Valid @RequestBody PatientDtos.RegisterMinorRequest request) {
        Views.PatientView minor = patientUseCase.registerMinor(
                new RegistrationCommands.RegisterMinorPatient(
                        request.firstName(), request.lastName(), request.birthDate(), request.gender()));
        return ResponseEntity.status(HttpStatus.CREATED).body(minor);
    }

    @GetMapping("/me/minors")
    @PreAuthorize("hasAuthority('iam.user.read')")
    @Operation(summary = "Lister les profils mineurs rattaches au parent / tuteur connecte")
    public List<Views.PatientView> listMyMinors() {
        return patientUseCase.listMyMinors();
    }

    @PostMapping("/me/import")
    @PreAuthorize("hasAuthority('iam.import.data')")
    @Operation(summary = "Importer ses donnees depuis une autre plateforme (CSV, JSON...)")
    public Views.ImportSummary importProfile(@Valid @RequestBody PatientDtos.ImportRequest request) {
        return dataImportUseCase.importMyProfile(new ProfileCommands.ImportExternalProfile(
                request.format(), request.content(), request.sourcePlatform()));
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Fichier illisible");
        }
    }
}
