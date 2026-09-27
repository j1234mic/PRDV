package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Partage d'un document medical avec un praticien.
 *
 * <p>Table dediee (et non colonne JSON) : le controle d'acces et la liste
 * « documents partages avec moi » sont des requetes a part entiere, qui
 * doivent rester indexables et paginables.
 */
@Entity
@Table(name = "profile_document_shares", indexes = {
        @Index(name = "idx_share_document", columnList = "document_id"),
        @Index(name = "idx_share_grantee", columnList = "grantee_user_id"),
        @Index(name = "idx_share_owner", columnList = "owner_user_id")
})
@Getter
@Setter
public class DocumentShareEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    /** Proprietaire du document (denormalise pour l'effacement RGPD). */
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "grantee_user_id", nullable = false)
    private Long granteeUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 25, nullable = false)
    private MedicalDocument.SharePermission permission;

    @Column(length = 255)
    private String reason;

    private Instant grantedAt;

    private Instant expiresAt;

    private Instant revokedAt;
}
