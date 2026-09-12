package com.prdv.rdv.iam.adapter.out.persistence.mapper;

import com.prdv.rdv.iam.adapter.out.persistence.entity.EstablishmentMembershipEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.KycDocumentEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.PractitionerContractEntity;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import com.prdv.rdv.iam.domain.model.verification.PractitionerContract;
import org.springframework.stereotype.Component;

@Component
public class VerificationPersistenceMapper {

    public KycDocumentEntity toEntity(KycDocument d) {
        KycDocumentEntity e = new KycDocumentEntity();
        e.setId(d.getId());
        e.setOwnerUserId(d.getOwnerUserId());
        e.setType(d.getType());
        e.setStorageKey(d.getStorageKey());
        e.setOriginalFilename(d.getOriginalFilename());
        e.setContentType(d.getContentType());
        e.setSizeBytes(d.getSizeBytes());
        e.setStatus(d.getStatus());
        e.setReviewNote(d.getReviewNote());
        e.setUploadedAt(d.getUploadedAt());
        e.setReviewedAt(d.getReviewedAt());
        return e;
    }

    public KycDocument toDomain(KycDocumentEntity e) {
        KycDocument d = new KycDocument();
        d.setId(e.getId());
        d.setOwnerUserId(e.getOwnerUserId());
        d.setType(e.getType());
        d.setStorageKey(e.getStorageKey());
        d.setOriginalFilename(e.getOriginalFilename());
        d.setContentType(e.getContentType());
        d.setSizeBytes(e.getSizeBytes());
        d.setStatus(e.getStatus());
        d.setReviewNote(e.getReviewNote());
        d.setUploadedAt(e.getUploadedAt());
        d.setReviewedAt(e.getReviewedAt());
        return d;
    }

    public PractitionerContractEntity toEntity(PractitionerContract d) {
        PractitionerContractEntity e = new PractitionerContractEntity();
        e.setId(d.getId());
        e.setPractitionerUserId(d.getPractitionerUserId());
        e.setVersion(d.getVersion());
        e.setContentHash(d.getContentHash());
        e.setAcceptedAt(d.getAcceptedAt());
        e.setIpAddress(d.getIpAddress());
        return e;
    }

    public PractitionerContract toDomain(PractitionerContractEntity e) {
        PractitionerContract d = new PractitionerContract();
        d.setId(e.getId());
        d.setPractitionerUserId(e.getPractitionerUserId());
        d.setVersion(e.getVersion());
        d.setContentHash(e.getContentHash());
        d.setAcceptedAt(e.getAcceptedAt());
        d.setIpAddress(e.getIpAddress());
        return d;
    }

    public EstablishmentMembershipEntity toEntity(EstablishmentMembership d) {
        EstablishmentMembershipEntity e = new EstablishmentMembershipEntity();
        e.setId(d.getId());
        e.setEstablishmentUserId(d.getEstablishmentUserId());
        e.setPractitionerUserId(d.getPractitionerUserId());
        e.setMemberRole(d.getMemberRole());
        e.setStatus(d.getStatus());
        e.setValidFrom(d.getValidFrom());
        e.setValidUntil(d.getValidUntil());
        e.setRequestedAt(d.getRequestedAt());
        e.setDecidedAt(d.getDecidedAt());
        return e;
    }

    public EstablishmentMembership toDomain(EstablishmentMembershipEntity e) {
        EstablishmentMembership d = new EstablishmentMembership();
        d.setId(e.getId());
        d.setEstablishmentUserId(e.getEstablishmentUserId());
        d.setPractitionerUserId(e.getPractitionerUserId());
        d.setMemberRole(e.getMemberRole());
        d.setStatus(e.getStatus());
        d.setValidFrom(e.getValidFrom());
        d.setValidUntil(e.getValidUntil());
        d.setRequestedAt(e.getRequestedAt());
        d.setDecidedAt(e.getDecidedAt());
        return d;
    }
}
