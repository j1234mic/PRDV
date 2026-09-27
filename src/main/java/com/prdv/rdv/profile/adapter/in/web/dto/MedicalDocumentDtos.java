package com.prdv.rdv.profile.adapter.in.web.dto;

import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

/** DTO d'entree des documents medicaux (televersement, versions, partage). */
public final class MedicalDocumentDtos {

    private MedicalDocumentDtos() {
    }

    public record UploadRequest(@NotBlank @Size(max = 255) String title,
                                MedicalDocument.DocumentCategory category,
                                @Size(max = 10) String dicomModality,
                                @Size(max = 255) String dicomStudyDescription) {

        public DocumentCommands.UploadDocument toCommand(MultipartFile file) {
            return new DocumentCommands.UploadDocument(title, category,
                    PatientIdentityDtos.filename(file), PatientIdentityDtos.contentType(file),
                    PatientIdentityDtos.content(file), dicomModality, dicomStudyDescription);
        }
    }

    public record AddVersionRequest(@Size(max = 500) String changeNote) {

        public DocumentCommands.AddVersion toCommand(Long documentId, MultipartFile file) {
            return new DocumentCommands.AddVersion(documentId, PatientIdentityDtos.filename(file),
                    PatientIdentityDtos.contentType(file), PatientIdentityDtos.content(file), changeNote);
        }
    }

    public record ReclassifyRequest(@NotNull MedicalDocument.DocumentCategory category) {

        public DocumentCommands.Reclassify toCommand(Long documentId) {
            return new DocumentCommands.Reclassify(documentId, category);
        }
    }

    public record ShareRequest(@NotNull Long granteeUserId,
                               @NotNull MedicalDocument.SharePermission permission,
                               @Size(max = 255) String reason,
                               Instant expiresAt) {

        public DocumentCommands.ShareDocument toCommand(Long documentId) {
            return new DocumentCommands.ShareDocument(documentId, granteeUserId, permission, reason,
                    expiresAt);
        }
    }

    public record PushToDmpRequest(Long documentId) {

        public DocumentCommands.PushToDmp toCommand() {
            return new DocumentCommands.PushToDmp(documentId);
        }
    }
}
