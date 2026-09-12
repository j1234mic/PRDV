package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "secretary_profiles")
@Getter
@Setter
public class SecretaryProfileEntity extends AbstractProfileEntity {

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @ElementCollection
    @CollectionTable(name = "secretary_supervised_practitioners",
            joinColumns = @JoinColumn(name = "secretary_profile_id"))
    @Column(name = "practitioner_user_id")
    private Set<Long> supervisedPractitionerIds = new HashSet<>();
}
