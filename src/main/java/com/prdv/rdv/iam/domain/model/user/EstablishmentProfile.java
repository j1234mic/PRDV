package com.prdv.rdv.iam.domain.model.user;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Profil etablissement de sante (cabinet, clinique) : compte entreprise
 * multi-praticiens avec gestion centrale et services/departements.
 */
@Getter
@Setter
public class EstablishmentProfile {

    private Long id;
    private Long userId;
    private String legalName;
    private String siret;
    private String address;

    /** Services / departements du cabinet (ex : « Cardiologie », « Imagerie »). */
    private List<String> departments = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    public static EstablishmentProfile create(Long userId, String legalName, String siret,
                                              String address, List<String> departments, Clock clock) {
        EstablishmentProfile e = new EstablishmentProfile();
        e.userId = userId;
        e.legalName = legalName;
        e.siret = siret;
        e.address = address;
        if (departments != null) {
            e.departments = new ArrayList<>(departments);
        }
        e.createdAt = clock.instant();
        e.updatedAt = e.createdAt;
        return e;
    }
}
