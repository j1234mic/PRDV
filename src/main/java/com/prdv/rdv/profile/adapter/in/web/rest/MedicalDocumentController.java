package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.MedicalDocumentDtos;
import com.prdv.rdv.profile.adapter.in.web.dto.PatientIdentityDtos;
import com.prdv.rdv.profile.application.port.input.DocumentSharingUseCase;
import com.prdv.rdv.profile.application.port.input.MedicalDocumentQueryUseCase;
import com.prdv.rdv.profile.application.port.input.MedicalDocumentUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Documents medicaux (module 2.1) : televersement multi-format (PDF, JPG,
 * DICOM), classification automatique par IA, OCR, versioning, partage securise
 * avec des praticiens et transmission au DMP national.
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents medicaux", description = "Upload, classification IA, OCR, versions, partage")
public class MedicalDocumentController {

    private final MedicalDocumentUseCase documentUseCase;
    private final MedicalDocumentQueryUseCase queryUseCase;
    private final DocumentSharingUseCase sharingUseCase;

    public MedicalDocumentController(MedicalDocumentUseCase documentUseCase,
                                     MedicalDocumentQueryUseCase queryUseCase,
                                     DocumentSharingUseCase sharingUseCase) {
        this.documentUseCase = documentUseCase;
        this.queryUseCase = queryUseCase;
        this.sharingUseCase = sharingUseCase;
    }

    // ------------------------------------------------------------------
    // Lectures et telechargement
    // ------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('profile.document.read')")
    public List<ProfileViews.MedicalDocumentView> myDocuments(
            @RequestParam(required = false) MedicalDocument.DocumentCategory category) {
        return queryUseCase.myDocuments(category);
    }

    @GetMapping("/shared-with-me")
    @PreAuthorize("hasAuthority('profile.document.read')")
    @Operation(summary = "Documents partages avec le demandeur (partages actifs uniquement)")
    public List<ProfileViews.MedicalDocumentView> sharedWithMe() {
        return queryUseCase.sharedWithMe();
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAuthority('profile.document.read')")
    public ProfileViews.MedicalDocumentView document(@PathVariable Long documentId) {
        return queryUseCase.document(documentId);
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAuthority('profile.document.read')")
    @Operation(summary = "Telechargement : le droit de lecture est verifie avant l'envoi des octets")
    public ResponseEntity<byte[]> download(@PathVariable Long documentId) {
        ProfileViews.DocumentFile file = queryUseCase.download(documentId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType()))
                .contentLength(file.sizeBytes())
                .body(file.content());
    }

    // ------------------------------------------------------------------
    // Televersement et versioning
    // ------------------------------------------------------------------

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.document.write')")
    @Operation(summary = "Televersement : empreinte SHA-256, classification IA et OCR automatiques")
    public ProfileViews.MedicalDocumentView upload(
            @RequestPart("document") @Valid MedicalDocumentDtos.UploadRequest request,
            @RequestPart("file") MultipartFile file) {
        return documentUseCase.upload(request.toCommand(file));
    }

    @PostMapping(value = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.document.write')")
    @Operation(summary = "Nouvelle version : la precedente reste consultable")
    public ProfileViews.MedicalDocumentView addVersion(
            @PathVariable Long documentId,
            @RequestPart(value = "version", required = false) MedicalDocumentDtos.AddVersionRequest request,
            @RequestPart("file") MultipartFile file) {
        MedicalDocumentDtos.AddVersionRequest effective = request == null
                ? new MedicalDocumentDtos.AddVersionRequest(null)
                : request;
        return documentUseCase.addVersion(effective.toCommand(documentId, file));
    }

    @PutMapping("/{documentId}/category")
    @PreAuthorize("hasAuthority('profile.document.write')")
    @Operation(summary = "Requalification manuelle (la classification automatique peut etre corrigee)")
    public ProfileViews.MedicalDocumentView reclassify(
            @PathVariable Long documentId,
            @Valid @RequestBody MedicalDocumentDtos.ReclassifyRequest request) {
        return documentUseCase.reclassify(request.toCommand(documentId));
    }

    @PostMapping("/{documentId}/archive")
    @PreAuthorize("hasAuthority('profile.document.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long documentId) {
        documentUseCase.archive(documentId);
    }

    // ------------------------------------------------------------------
    // Partage securise et DMP
    // ------------------------------------------------------------------

    @PostMapping("/{documentId}/shares")
    @PreAuthorize("hasAuthority('profile.document.share')")
    public ProfileViews.DocumentShareView share(
            @PathVariable Long documentId,
            @Valid @RequestBody MedicalDocumentDtos.ShareRequest request) {
        return sharingUseCase.share(request.toCommand(documentId));
    }

    @GetMapping("/{documentId}/shares")
    @PreAuthorize("hasAuthority('profile.document.share')")
    public List<ProfileViews.DocumentShareView> sharesOf(@PathVariable Long documentId) {
        return sharingUseCase.sharesOf(documentId);
    }

    @DeleteMapping("/{documentId}/shares/{shareId}")
    @PreAuthorize("hasAuthority('profile.document.share')")
    public ProfileViews.DocumentShareView revokeShare(@PathVariable Long documentId,
                                                      @PathVariable String shareId) {
        return sharingUseCase.revokeShare(documentId, shareId);
    }

    @PostMapping("/{documentId}/dmp")
    @PreAuthorize("hasAuthority('profile.document.share')")
    @Operation(summary = "Transmission au DMP national (consentement DMP requis)")
    public ProfileViews.MedicalDocumentView pushToDmp(@PathVariable Long documentId) {
        return sharingUseCase.pushToDmp(new MedicalDocumentDtos.PushToDmpRequest(documentId).toCommand());
    }
}
