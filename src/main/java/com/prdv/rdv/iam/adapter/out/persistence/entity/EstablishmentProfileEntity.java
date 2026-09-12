package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "establishment_profiles")
@Getter
@Setter
public class EstablishmentProfileEntity extends AbstractProfileEntity {

    @Column(length = 150)
    private String legalName;

    @Column(length = 20)
    private String siret;

    @Column(length = 255)
    private String address;

    @ElementCollection
    @CollectionTable(name = "establishment_departments",
            joinColumns = @JoinColumn(name = "establishment_profile_id"))
    @Column(name = "department", length = 100)
    private List<String> departments = new ArrayList<>();
}
