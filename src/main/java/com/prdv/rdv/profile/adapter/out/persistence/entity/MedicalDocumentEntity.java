package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.adapter.out.persistence.converter.JsonConverters;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistance des documents medicaux : metadonnees, resultats de
 * classification et d'OCR, historique de versions immuables et partages.
 *
 * <p>Le contenu binaire vit dans le stockage externe : seules les cles et les
 * empreintes SHA-256 sont persistees ici.
 */
@Entity
@Table(name = "profile_medical_documents", indexes = {
        @Index(name = "idx_document_owner", columnList = "owner_user_id"),
        @Index(name = "idx_document_category", columnList = "category")
})
@Getter
@Setter
public class MedicalDocumentEntity extends ProfileEntity {

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MedicalDocument.DocumentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MedicalDocument.DocumentStatus status;

    // --- Classification ---------------------------------------------------
    @Column(precision = 4, scale = 3)
    private Double classificationConfidence;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MedicalDocument.ClassificationSource classificationSource;

    @Column(length = 60)
    private String classifierName;

    private Instant classifiedAt;

    // --- OCR ---------------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private MedicalDocument.OcrStatus ocrStatus;

    @Lob
    @Column(name = "ocr_raw_text")
    private String ocrRawText;

    @Lob
    @Convert(converter = JsonConverters.OcrFields.class)
    @Column(name = "ocr_extracted_fields")
    private Map<String, String> ocrExtractedFields = new LinkedHashMap<>();

    @Column(precision = 4, scale = 3)
    private Double ocrConfidence;

    @Column(length = 40)
    private String ocrEngine;

    private Instant ocrProcessedAt;

    // --- DICOM / DMP --------------------------------------------------------
    @Column(length = 10)
    private String dicomModality;

    @Column(length = 255)
    private String dicomStudyDescription;

    @Column(length = 80)
    private String dmpReference;

    private Instant dmpSharedAt;

    // --- Versions et partages ------------------------------------------------
    @Lob
    @Convert(converter = JsonConverters.DocumentVersionList.class)
    @Column(name = "versions")
    private List<MedicalDocument.DocumentVersion> versions = new ArrayList<>();

    private Instant archivedAt;
}
