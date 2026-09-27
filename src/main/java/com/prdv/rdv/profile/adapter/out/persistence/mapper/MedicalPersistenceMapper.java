package com.prdv.rdv.profile.adapter.out.persistence.mapper;

import com.prdv.rdv.profile.adapter.out.persistence.entity.DocumentShareEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.MedicalDocumentEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.MedicalRecordEntity;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import com.prdv.rdv.profile.domain.model.medical.BloodGroup;
import com.prdv.rdv.profile.domain.model.medical.BodyMetrics;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import com.prdv.rdv.profile.domain.model.medical.VitalSigns;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Mapper domaine &lt;-&gt; entites JPA pour le dossier medical et les documents. */
@Component
public class MedicalPersistenceMapper {

    // ------------------------------------------------------------------
    // MedicalRecord
    // ------------------------------------------------------------------

    public MedicalRecordEntity toEntity(MedicalRecord record) {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(record.getId());
        entity.setPatientUserId(record.getPatientUserId());
        entity.setBloodGroup(record.getBloodGroup() == null ? null : record.getBloodGroup().toString());
        entity.setHistory(new ArrayList<>(record.getHistory()));
        entity.setFamilyHistory(new ArrayList<>(record.getFamilyHistory()));
        entity.setAllergies(new ArrayList<>(record.getAllergies()));
        entity.setVaccinations(new ArrayList<>(record.getVaccinations()));
        entity.setChronicConditions(new ArrayList<>(record.getChronicConditions()));
        entity.setTreatments(new ArrayList<>(record.getTreatments()));
        entity.setSurgeries(new ArrayList<>(record.getSurgeries()));
        entity.setHospitalizations(new ArrayList<>(record.getHospitalizations()));
        entity.setDisabilities(new ArrayList<>(record.getDisabilities()));

        MedicalRecord.VitalSignsSnapshot snapshot = record.getLastVitalSigns();
        if (snapshot != null && snapshot.vitalSigns() != null) {
            VitalSigns signs = snapshot.vitalSigns();
            entity.setSystolicMmHg(signs.systolicMmHg());
            entity.setDiastolicMmHg(signs.diastolicMmHg());
            entity.setHeartRateBpm(signs.heartRateBpm());
            entity.setTemperatureCelsius(signs.temperatureCelsius());
            entity.setOxygenSaturationPercent(signs.oxygenSaturationPercent());
            entity.setRespiratoryRatePerMin(signs.respiratoryRatePerMin());
            if (signs.bodyMetrics() != null) {
                entity.setHeightCm(signs.bodyMetrics().heightCm());
                entity.setWeightKg(signs.bodyMetrics().weightKg());
            }
            entity.setVitalsMeasuredAt(signs.measuredAt());
            entity.setVitalsRecordedAt(snapshot.recordedAt());
        }

        entity.setCreatedAt(record.getCreatedAt());
        entity.setUpdatedAt(record.getUpdatedAt());
        return entity;
    }

    public MedicalRecord toDomain(MedicalRecordEntity entity) {
        MedicalRecord record = new MedicalRecord();
        record.setId(entity.getId());
        record.setPatientUserId(entity.getPatientUserId());
        record.setBloodGroup(entity.getBloodGroup() == null ? null : BloodGroup.of(entity.getBloodGroup()));
        record.setHistory(copy(entity.getHistory()));
        record.setFamilyHistory(copy(entity.getFamilyHistory()));
        record.setAllergies(copy(entity.getAllergies()));
        record.setVaccinations(copy(entity.getVaccinations()));
        record.setChronicConditions(copy(entity.getChronicConditions()));
        record.setTreatments(copy(entity.getTreatments()));
        record.setSurgeries(copy(entity.getSurgeries()));
        record.setHospitalizations(copy(entity.getHospitalizations()));
        record.setDisabilities(copy(entity.getDisabilities()));

        if (entity.getVitalsMeasuredAt() != null) {
            BodyMetrics bodyMetrics = entity.getHeightCm() != null && entity.getWeightKg() != null
                    ? new BodyMetrics(entity.getHeightCm(), entity.getWeightKg())
                    : null;
            VitalSigns signs = new VitalSigns(entity.getSystolicMmHg(), entity.getDiastolicMmHg(),
                    entity.getHeartRateBpm(), entity.getTemperatureCelsius(),
                    entity.getOxygenSaturationPercent(), entity.getRespiratoryRatePerMin(), bodyMetrics,
                    entity.getVitalsMeasuredAt());
            record.setLastVitalSigns(new MedicalRecord.VitalSignsSnapshot(signs,
                    entity.getVitalsRecordedAt() == null ? entity.getVitalsMeasuredAt()
                            : entity.getVitalsRecordedAt()));
        }

        record.setCreatedAt(entity.getCreatedAt());
        record.setUpdatedAt(entity.getUpdatedAt());
        return record;
    }

    // ------------------------------------------------------------------
    // MedicalDocument
    // ------------------------------------------------------------------

    public MedicalDocumentEntity toEntity(MedicalDocument document) {
        MedicalDocumentEntity entity = new MedicalDocumentEntity();
        entity.setId(document.getId());
        entity.setOwnerUserId(document.getOwnerUserId());
        entity.setTitle(document.getTitle());
        entity.setCategory(document.getCategory());
        entity.setStatus(document.getStatus());

        MedicalDocument.ClassificationResult classification = document.getClassification();
        if (classification != null) {
            entity.setClassificationConfidence(classification.confidence());
            entity.setClassificationSource(classification.source());
            entity.setClassifierName(classification.classifier());
            entity.setClassifiedAt(classification.classifiedAt());
        }

        MedicalDocument.OcrResult ocr = document.getOcrResult();
        if (ocr != null) {
            entity.setOcrStatus(ocr.status());
            entity.setOcrRawText(ocr.rawText());
            entity.setOcrExtractedFields(new java.util.LinkedHashMap<>(ocr.extractedFields()));
            entity.setOcrConfidence(ocr.confidence());
            entity.setOcrEngine(ocr.engine());
            entity.setOcrProcessedAt(ocr.processedAt());
        }

        entity.setDicomModality(document.getDicomModality());
        entity.setDicomStudyDescription(document.getDicomStudyDescription());
        entity.setDmpReference(document.getDmpReference());
        entity.setDmpSharedAt(document.getDmpSharedAt());
        entity.setVersions(new ArrayList<>(document.getVersions()));
        entity.setArchivedAt(document.getArchivedAt());
        entity.setCreatedAt(document.getCreatedAt());
        entity.setUpdatedAt(document.getUpdatedAt());
        return entity;
    }

    public MedicalDocument toDomain(MedicalDocumentEntity entity, List<DocumentShareEntity> shares) {
        MedicalDocument document = new MedicalDocument();
        document.setId(entity.getId());
        document.setOwnerUserId(entity.getOwnerUserId());
        document.setTitle(entity.getTitle());
        document.setCategory(entity.getCategory());
        document.setStatus(entity.getStatus());

        if (entity.getClassificationSource() != null && entity.getCategory() != null) {
            document.setClassification(new MedicalDocument.ClassificationResult(entity.getCategory(),
                    entity.getClassificationConfidence() == null ? 0 : entity.getClassificationConfidence(),
                    entity.getClassificationSource(), entity.getClassifierName(),
                    entity.getClassifiedAt()));
        }
        if (entity.getOcrStatus() != null) {
            document.setOcrResult(new MedicalDocument.OcrResult(entity.getOcrStatus(),
                    entity.getOcrRawText(),
                    entity.getOcrExtractedFields() == null ? java.util.Map.of()
                            : entity.getOcrExtractedFields(),
                    entity.getOcrConfidence() == null ? 0 : entity.getOcrConfidence(),
                    entity.getOcrEngine(), entity.getOcrProcessedAt()));
        }

        document.setDicomModality(entity.getDicomModality());
        document.setDicomStudyDescription(entity.getDicomStudyDescription());
        document.setDmpReference(entity.getDmpReference());
        document.setDmpSharedAt(entity.getDmpSharedAt());
        document.setVersions(copy(entity.getVersions()));
        document.setShares(new LinkedHashSet<>());
        if (shares != null) {
            shares.forEach(share -> document.getShares().add(toDomain(share)));
        }
        document.setArchivedAt(entity.getArchivedAt());
        document.setCreatedAt(entity.getCreatedAt());
        document.setUpdatedAt(entity.getUpdatedAt());
        return document;
    }

    public DocumentShareEntity toEntity(MedicalDocument.DocumentShare share, Long documentId,
                                        Long ownerUserId) {
        DocumentShareEntity entity = new DocumentShareEntity();
        entity.setId(share.id());
        entity.setDocumentId(documentId);
        entity.setOwnerUserId(ownerUserId);
        entity.setGranteeUserId(share.granteeUserId());
        entity.setPermission(share.permission());
        entity.setReason(share.reason());
        entity.setGrantedAt(share.grantedAt());
        entity.setExpiresAt(share.expiresAt());
        entity.setRevokedAt(share.revokedAt());
        return entity;
    }

    public MedicalDocument.DocumentShare toDomain(DocumentShareEntity entity) {
        return new MedicalDocument.DocumentShare(entity.getId(), entity.getGranteeUserId(),
                entity.getPermission(), entity.getReason(), entity.getGrantedAt(), entity.getExpiresAt(),
                entity.getRevokedAt());
    }

    private static <T> List<T> copy(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
