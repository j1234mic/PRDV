package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.adapter.out.persistence.converter.JsonConverters;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistance du dossier professionnel du praticien (identite, informations
 * medicales, gestion, visibilite, reseau).
 */
@Entity
@Table(name = "profile_practitioner_dossiers", indexes = {
        @Index(name = "idx_dossier_user", columnList = "user_id", unique = true),
        @Index(name = "idx_dossier_specialty", columnList = "main_specialty")
})
@Getter
@Setter
public class PractitionerDossierEntity extends ProfileEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // --- Identite professionnelle -------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private PractitionerDossier.Title title;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 20)
    private String rppsNumber;

    @Column(length = 20)
    private String adeliNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PractitionerDossier.RegistrationOrder registrationOrder;

    @Column(name = "main_specialty", length = 120)
    private String mainSpecialty;

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "secondary_specialties")
    private List<String> secondarySpecialties = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "sub_specialties")
    private List<String> subSpecialties = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "skills")
    private List<String> skills = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.DiplomaList.class)
    @Column(name = "diplomas")
    private List<PractitionerDossier.Diploma> diplomas = new ArrayList<>();

    private Integer experienceYears;

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "languages")
    private List<String> languages = new ArrayList<>();

    @Column(length = 500)
    private String photoStorageKey;

    @Column(length = 500)
    private String presentationVideoKey;

    @Column(length = 1000)
    private String shortBio;

    // --- Informations medicales ------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private PractitionerDossier.ConventionSector sector;

    @Lob
    @Convert(converter = JsonConverters.TariffList.class)
    @Column(name = "tariffs")
    private List<PractitionerDossier.Tariff> tariffs = new ArrayList<>();

    private boolean optam;

    private boolean optamCo;

    private LocalDate optamSignedOn;

    @Lob
    @Convert(converter = JsonConverters.PaymentMethodSet.class)
    @Column(name = "payment_methods")
    private Set<PractitionerDossier.PaymentMethod> paymentMethods = new LinkedHashSet<>();

    private boolean thirdPartyPayment;

    @Column(length = 500)
    private String thirdPartyPaymentConditions;

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "acts_performed")
    private List<String> actsPerformed = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "available_equipment")
    private List<String> availableEquipment = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "treated_pathologies")
    private List<String> treatedPathologies = new ArrayList<>();

    private Integer acceptedAgeMin;

    private Integer acceptedAgeMax;

    private boolean teleconsultation;

    @Column(length = 120)
    private String teleconsultationPlatform;

    // --- Gestion professionnelle --------------------------------------------------
    @Column(length = 14)
    private String siret;

    @Column(length = 64)
    private String ribToken;

    @Column(length = 40)
    private String maskedIban;

    @Column(length = 120)
    private String professionalInsurer;

    @Column(length = 80)
    private String insurancePolicyNumber;

    private LocalDate insuranceExpiresOn;

    @Column(length = 40)
    private String urssafNumber;

    @Column(length = 120)
    private String accountingAssociation;

    @Column(length = 40)
    private String accountingAssociationNumber;

    private Integer ordinalCotisationYear;

    @Column(precision = 10, scale = 2)
    private BigDecimal ordinalCotisationAmount;

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "quality_certifications")
    private List<String> qualityCertifications = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.AccreditationList.class)
    @Column(name = "accreditations")
    private List<PractitionerDossier.Accreditation> accreditations = new ArrayList<>();

    // --- Visibilite ------------------------------------------------------------------
    @Lob
    @Column(name = "long_description")
    private String longDescription;

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "expertise_domains")
    private List<String> expertiseDomains = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "publications")
    private List<String> publications = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "distinctions")
    private List<String> distinctions = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "conference_talks")
    private List<String> conferenceTalks = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "media_references")
    private List<String> mediaReferences = new ArrayList<>();

    // --- Réseau ------------------------------------------------------------------------
    @Lob
    @Convert(converter = JsonConverters.NetworkContactList.class)
    @Column(name = "network_contacts")
    private List<PractitionerDossier.NetworkContact> networkContacts = new ArrayList<>();
}
