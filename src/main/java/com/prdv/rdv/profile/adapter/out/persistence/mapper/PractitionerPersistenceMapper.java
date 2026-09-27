package com.prdv.rdv.profile.adapter.out.persistence.mapper;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PracticeLocationEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.PractitionerDossierEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.PractitionerRatingEntity;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerRating;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/** Mapper domaine &lt;-&gt; entites JPA pour le contexte praticien. */
@Component
public class PractitionerPersistenceMapper {

    // ------------------------------------------------------------------
    // PractitionerDossier
    // ------------------------------------------------------------------

    public PractitionerDossierEntity toEntity(PractitionerDossier dossier) {
        PractitionerDossierEntity entity = new PractitionerDossierEntity();
        entity.setId(dossier.getId());
        entity.setUserId(dossier.getUserId());

        PractitionerDossier.ProfessionalIdentity identity = dossier.getIdentity();
        if (identity != null) {
            entity.setTitle(identity.title());
            entity.setFirstName(identity.firstName());
            entity.setLastName(identity.lastName());
            entity.setRppsNumber(identity.rppsNumber());
            entity.setAdeliNumber(identity.adeliNumber());
            entity.setRegistrationOrder(identity.registrationOrder());
            entity.setMainSpecialty(identity.mainSpecialty());
            entity.setSecondarySpecialties(new ArrayList<>(identity.secondarySpecialties()));
            entity.setSubSpecialties(new ArrayList<>(identity.subSpecialties()));
            entity.setSkills(new ArrayList<>(identity.skills()));
            entity.setDiplomas(new ArrayList<>(identity.diplomas()));
            entity.setExperienceYears(identity.experienceYears());
            entity.setLanguages(new ArrayList<>(identity.languages()));
            entity.setPhotoStorageKey(identity.photoStorageKey());
            entity.setPresentationVideoKey(identity.presentationVideoKey());
            entity.setShortBio(identity.shortBio());
        }

        PractitionerDossier.PracticeInformation information = dossier.getPracticeInformation();
        if (information != null) {
            entity.setSector(information.sector());
            entity.setTariffs(new ArrayList<>(information.tariffs()));
            entity.setOptam(information.optam());
            entity.setOptamCo(information.optamCo());
            entity.setOptamSignedOn(information.optamSignedOn());
            entity.setPaymentMethods(new LinkedHashSet<>(information.paymentMethods()));
            entity.setThirdPartyPayment(information.thirdPartyPayment());
            entity.setThirdPartyPaymentConditions(information.thirdPartyPaymentConditions());
            entity.setActsPerformed(new ArrayList<>(information.actsPerformed()));
            entity.setAvailableEquipment(new ArrayList<>(information.availableEquipment()));
            entity.setTreatedPathologies(new ArrayList<>(information.treatedPathologies()));
            if (information.acceptedAges() != null) {
                entity.setAcceptedAgeMin(information.acceptedAges().minAge());
                entity.setAcceptedAgeMax(information.acceptedAges().maxAge());
            }
            entity.setTeleconsultation(information.teleconsultation());
            entity.setTeleconsultationPlatform(information.teleconsultationPlatform());
        }

        PractitionerDossier.ProfessionalManagement management = dossier.getManagement();
        if (management != null) {
            entity.setSiret(management.siret());
            entity.setRibToken(management.ribToken());
            entity.setMaskedIban(management.maskedIban());
            entity.setProfessionalInsurer(management.professionalInsurer());
            entity.setInsurancePolicyNumber(management.insurancePolicyNumber());
            entity.setInsuranceExpiresOn(management.insuranceExpiresOn());
            entity.setUrssafNumber(management.urssafNumber());
            entity.setAccountingAssociation(management.accountingAssociation());
            entity.setAccountingAssociationNumber(management.accountingAssociationNumber());
            entity.setOrdinalCotisationYear(management.ordinalCotisationYear());
            entity.setOrdinalCotisationAmount(management.ordinalCotisationAmount());
            entity.setQualityCertifications(new ArrayList<>(management.qualityCertifications()));
            entity.setAccreditations(new ArrayList<>(management.accreditations()));
        }

        PractitionerDossier.VisibilityProfile visibility = dossier.getVisibility();
        if (visibility != null) {
            entity.setLongDescription(visibility.longDescription());
            entity.setExpertiseDomains(new ArrayList<>(visibility.expertiseDomains()));
            entity.setPublications(new ArrayList<>(visibility.publications()));
            entity.setDistinctions(new ArrayList<>(visibility.distinctions()));
            entity.setConferenceTalks(new ArrayList<>(visibility.conferenceTalks()));
            entity.setMediaReferences(new ArrayList<>(visibility.mediaReferences()));
        }

        entity.setNetworkContacts(new ArrayList<>(dossier.getNetworkContacts()));
        entity.setCreatedAt(dossier.getCreatedAt());
        entity.setUpdatedAt(dossier.getUpdatedAt());
        return entity;
    }

    public PractitionerDossier toDomain(PractitionerDossierEntity entity) {
        PractitionerDossier dossier = new PractitionerDossier();
        dossier.setId(entity.getId());
        dossier.setUserId(entity.getUserId());

        if (entity.getLastName() != null) {
            dossier.setIdentity(new PractitionerDossier.ProfessionalIdentity(entity.getTitle(),
                    entity.getFirstName(), entity.getLastName(), entity.getRppsNumber(),
                    entity.getAdeliNumber(), entity.getRegistrationOrder(), entity.getMainSpecialty(),
                    copy(entity.getSecondarySpecialties()), copy(entity.getSubSpecialties()),
                    copy(entity.getSkills()), copy(entity.getDiplomas()), entity.getExperienceYears(),
                    copy(entity.getLanguages()), entity.getPhotoStorageKey(),
                    entity.getPresentationVideoKey(), entity.getShortBio()));
        }

        if (entity.getSector() != null) {
            dossier.setPracticeInformation(new PractitionerDossier.PracticeInformation(entity.getSector(),
                    copy(entity.getTariffs()), entity.isOptam(), entity.isOptamCo(),
                    entity.getOptamSignedOn(),
                    entity.getPaymentMethods() == null ? new LinkedHashSet<>()
                            : new LinkedHashSet<>(entity.getPaymentMethods()),
                    entity.isThirdPartyPayment(), entity.getThirdPartyPaymentConditions(),
                    copy(entity.getActsPerformed()), copy(entity.getAvailableEquipment()),
                    copy(entity.getTreatedPathologies()),
                    new PractitionerDossier.AgeRange(entity.getAcceptedAgeMin(),
                            entity.getAcceptedAgeMax()),
                    entity.isTeleconsultation(), entity.getTeleconsultationPlatform()));
        }

        if (entity.getSiret() != null || entity.getProfessionalInsurer() != null
                || entity.getRibToken() != null) {
            dossier.setManagement(new PractitionerDossier.ProfessionalManagement(entity.getSiret(),
                    entity.getRibToken(), entity.getMaskedIban(), entity.getProfessionalInsurer(),
                    entity.getInsurancePolicyNumber(), entity.getInsuranceExpiresOn(),
                    entity.getUrssafNumber(), entity.getAccountingAssociation(),
                    entity.getAccountingAssociationNumber(), entity.getOrdinalCotisationYear(),
                    entity.getOrdinalCotisationAmount(), copy(entity.getQualityCertifications()),
                    copy(entity.getAccreditations())));
        }

        if (entity.getLongDescription() != null || !isEmpty(entity.getExpertiseDomains())) {
            dossier.setVisibility(new PractitionerDossier.VisibilityProfile(entity.getLongDescription(),
                    copy(entity.getExpertiseDomains()), copy(entity.getPublications()),
                    copy(entity.getDistinctions()), copy(entity.getConferenceTalks()),
                    copy(entity.getMediaReferences())));
        }

        dossier.setNetworkContacts(copy(entity.getNetworkContacts()));
        dossier.setCreatedAt(entity.getCreatedAt());
        dossier.setUpdatedAt(entity.getUpdatedAt());
        return dossier;
    }

    // ------------------------------------------------------------------
    // PracticeLocation
    // ------------------------------------------------------------------

    public PracticeLocationEntity toEntity(PracticeLocation location) {
        PracticeLocationEntity entity = new PracticeLocationEntity();
        entity.setId(location.getId());
        entity.setPractitionerUserId(location.getPractitionerUserId());
        entity.setName(location.getName());
        entity.setMainLocation(location.isMainLocation());
        entity.setLine1(location.getLine1());
        entity.setLine2(location.getLine2());
        entity.setPostalCode(location.getPostalCode());
        entity.setCity(location.getCity());
        entity.setCountry(location.getCountry());
        entity.setLatitude(location.getLatitude());
        entity.setLongitude(location.getLongitude());
        entity.setOpeningHours(new ArrayList<>(location.getOpeningHours()));
        entity.setPhotos(new ArrayList<>(location.getPhotos()));
        entity.setSocialLinks(new LinkedHashMap<>(location.getSocialLinks()));
        entity.setVirtualTourUrl(location.getVirtualTourUrl());
        entity.setPhone(location.getPhone());
        entity.setMobilePhone(location.getMobilePhone());
        entity.setFax(location.getFax());
        entity.setEmail(location.getEmail());
        entity.setWebsite(location.getWebsite());
        entity.setWheelchairAccessible(location.isWheelchairAccessible());
        entity.setParkingAvailable(location.isParkingAvailable());
        entity.setPublicTransportInfo(location.getPublicTransportInfo());
        entity.setCreatedAt(location.getCreatedAt());
        entity.setUpdatedAt(location.getUpdatedAt());
        return entity;
    }

    public PracticeLocation toDomain(PracticeLocationEntity entity) {
        PracticeLocation location = new PracticeLocation();
        location.setId(entity.getId());
        location.setPractitionerUserId(entity.getPractitionerUserId());
        location.setName(entity.getName());
        location.setMainLocation(entity.isMainLocation());
        location.setLine1(entity.getLine1());
        location.setLine2(entity.getLine2());
        location.setPostalCode(entity.getPostalCode());
        location.setCity(entity.getCity());
        location.setCountry(entity.getCountry());
        location.setLatitude(entity.getLatitude());
        location.setLongitude(entity.getLongitude());
        location.setOpeningHours(copy(entity.getOpeningHours()));
        location.setPhotos(copy(entity.getPhotos()));
        location.setSocialLinks(entity.getSocialLinks() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(entity.getSocialLinks()));
        location.setVirtualTourUrl(entity.getVirtualTourUrl());
        location.setPhone(entity.getPhone());
        location.setMobilePhone(entity.getMobilePhone());
        location.setFax(entity.getFax());
        location.setEmail(entity.getEmail());
        location.setWebsite(entity.getWebsite());
        location.setWheelchairAccessible(entity.isWheelchairAccessible());
        location.setParkingAvailable(entity.isParkingAvailable());
        location.setPublicTransportInfo(entity.getPublicTransportInfo());
        location.setCreatedAt(entity.getCreatedAt());
        location.setUpdatedAt(entity.getUpdatedAt());
        return location;
    }

    // ------------------------------------------------------------------
    // PractitionerRating
    // ------------------------------------------------------------------

    public PractitionerRatingEntity toEntity(PractitionerRating rating) {
        PractitionerRatingEntity entity = new PractitionerRatingEntity();
        entity.setId(rating.getId());
        entity.setPractitionerUserId(rating.getPractitionerUserId());
        entity.setPatientUserId(rating.getPatientUserId());
        entity.setScore(rating.getScore());
        entity.setComment(rating.getComment());
        entity.setCreatedAt(rating.getCreatedAt());
        entity.setHidden(rating.isHidden());
        entity.setModerationNote(rating.getModerationNote());
        entity.setModeratedAt(rating.getModeratedAt());
        return entity;
    }

    public PractitionerRating toDomain(PractitionerRatingEntity entity) {
        PractitionerRating rating = new PractitionerRating();
        rating.setId(entity.getId());
        rating.setPractitionerUserId(entity.getPractitionerUserId());
        rating.setPatientUserId(entity.getPatientUserId());
        rating.setScore(entity.getScore());
        rating.setComment(entity.getComment());
        rating.setCreatedAt(entity.getCreatedAt());
        rating.setHidden(entity.isHidden());
        rating.setModerationNote(entity.getModerationNote());
        rating.setModeratedAt(entity.getModeratedAt());
        return rating;
    }

    private static <T> List<T> copy(List<T> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private static boolean isEmpty(List<?> source) {
        return source == null || source.isEmpty();
    }
}
