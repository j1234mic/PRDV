package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "kyc_documents", indexes = {
        @Index(name = "idx_kyc_owner", columnList = "owner_user_id"),
        @Index(name = "idx_kyc_status", columnList = "status")
})
@Getter
@Setter
public class KycDocumentEntity extends TimestampedEntity {

    @Column(nullable = false)
    private Long ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KycDocument.DocumentType type;

    @Column(length = 500)
    private String storageKey;

    @Column(length = 255)
    private String originalFilename;

    @Column(length = 100)
    private String contentType;

    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private KycDocument.ReviewStatus status;

    @Column(length = 500)
    private String reviewNote;

    private Instant uploadedAt;
    private Instant reviewedAt;
}
