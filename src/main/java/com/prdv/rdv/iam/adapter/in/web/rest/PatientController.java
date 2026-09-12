package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.PatientDtos;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.port.input.DataImportUseCase;
import com.prdv.rdv.iam.application.port.input.PatientRegistrationUseCase;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "KYC piece d'identite et import depuis autre plateforme")
public class PatientController {

    private final PatientRegistrationUseCase patientUseCase;
    private final DataImportUseCase dataImportUseCase;

    public PatientController(PatientRegistrationUseCase patientUseCase,
                             DataImportUseCase dataImportUseCase) {
        this.patientUseCase = patientUseCase;
        this.dataImportUseCase = dataImportUseCase;
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
