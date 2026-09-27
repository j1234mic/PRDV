package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.adapter.out.persistence.converter.JsonConverters;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistance de l'identite patient (etat civil, coordonnees, couverture,
 * carte Vitale, DMP).
 *
 * <p>Les collections de Value Objects (contacts, adresses, couvertures) sont
 * serialisees en JSON : elles n'existent qu'avec leur agregat. Le numero de
 * securite sociale n'est jamais stocke en clair, seulement sous forme de
 * jeton et de valeur masquee.
 */
@Entity
@Table(name = "profile_patient_identities", indexes = {
        @Index(name = "idx_patient_identity_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
public class PatientIdentityEntity extends ProfileEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // --- Etat civil -----------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PatientIdentity.Civility civility;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String birthName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 100)
    private String preferredName;

    private LocalDate birthDate;

    @Column(length = 120)
    private String birthPlace;

    @Column(length = 3)
    private String birthCountry;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private PatientIdentity.Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private PatientIdentity.MaritalStatus maritalStatus;

    @Column(length = 3)
    private String nationality;

    // --- Coordonnees ----------------------------------------------------
    @Lob
    @Convert(converter = JsonConverters.ContactPointList.class)
    @Column(name = "contacts")
    private List<PatientIdentity.ContactPoint> contacts = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.PostalAddressList.class)
    @Column(name = "addresses")
    private List<PatientIdentity.PostalAddress> addresses = new ArrayList<>();

    @Column(length = 100)
    private String emergencyFirstName;

    @Column(length = 100)
    private String emergencyLastName;

    @Column(length = 60)
    private String emergencyRelationship;

    @Column(length = 30)
    private String emergencyPhone;

    @Column(length = 160)
    private String emergencyEmail;

    @Column(length = 100)
    private String physicianFirstName;

    @Column(length = 100)
    private String physicianLastName;

    @Column(length = 20)
    private String physicianRppsNumber;

    @Column(length = 30)
    private String physicianPhone;

    @Column(length = 160)
    private String physicianEmail;

    private boolean physicianDeclaredToInsurance;

    // --- Donnees sensibles (jamais en clair) -----------------------------
    @Column(length = 64)
    private String socialSecurityNumberToken;

    @Column(length = 32)
    private String maskedSocialSecurityNumber;

    @Column(length = 500)
    private String photoStorageKey;

    @Column(length = 500)
    private String identityDocumentStorageKey;

    @Column(length = 64)
    private String identityDocumentToken;

    // --- Carte Vitale ----------------------------------------------------
    @Column(length = 64)
    private String vitaleNirToken;

    @Column(length = 20)
    private String vitaleCardVersion;

    private LocalDate vitaleIssuedOn;

    private LocalDate vitaleExpiresOn;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PatientIdentity.VitaleReadMode vitaleReadMode;

    @Column(length = 500)
    private String vitaleScanStorageKey;

    private Instant vitaleReadAt;

    // --- Couvertures ------------------------------------------------------
    @Lob
    @Convert(converter = JsonConverters.HealthInsuranceList.class)
    @Column(name = "insurances")
    private List<PatientIdentity.HealthInsurance> insurances = new ArrayList<>();

    // --- DMP --------------------------------------------------------------
    @Column(length = 80)
    private String dmpIdentifier;

    private boolean dmpLinked;

    private boolean dmpSharingEnabled;

    private Instant dmpLinkedAt;

    private Instant dmpLastSyncAt;
}
