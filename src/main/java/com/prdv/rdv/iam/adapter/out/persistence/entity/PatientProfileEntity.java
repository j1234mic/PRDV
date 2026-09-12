package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
public class PatientProfileEntity extends AbstractProfileEntity {

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PatientProfile.Gender gender;

    private Long guardianUserId;

    @Column(length = 255)
    private String identityDocumentToken;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PatientProfile.KycLevel kycLevel;

    @Column(length = 64)
    private String importedFrom;

    // Les donnees medicales importables seront ajoutees par les modules suivants ;
    // la collection ci-dessous montre le point d'extension (anti-corruption d'import).
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "patient_import_attributes",
            joinColumns = @JoinColumn(name = "patient_profile_id"))
    @Column(name = "attribute_value", length = 255)
    private java.util.List<String> importedAttributes = new java.util.ArrayList<>();
}
