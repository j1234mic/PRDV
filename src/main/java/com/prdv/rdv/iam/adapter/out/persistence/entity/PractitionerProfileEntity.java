package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "practitioner_profiles")
@Getter
@Setter
public class PractitionerProfileEntity extends AbstractProfileEntity {

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 120)
    private String specialty;

    @Column(length = 20)
    private String rppsNumber;

    @Column(length = 20)
    private String adeliNumber;

    private boolean rppsVerified;
    private boolean adeliVerified;
    private boolean diplomaVerified;
    private boolean professionalInsuranceVerified;
    private boolean bankAccountVerified;

    @Column(length = 255)
    private String ribToken;

    @Column(length = 40)
    private String maskedIban;

    @Column(length = 500)
    private String validationNote;
}
