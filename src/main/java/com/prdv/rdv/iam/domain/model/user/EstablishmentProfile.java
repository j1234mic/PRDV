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

    /** Remplace la liste des services/departements de l'etablissement (sans doublons ni vides). */
    public void updateDepartments(List<String> newDepartments, Clock clock) {
        List<String> cleaned = new ArrayList<>();
        if (newDepartments != null) {
            for (String dept : newDepartments) {
                if (dept != null && !dept.isBlank()) {
                    String trimmed = dept.trim();
                    if (!cleaned.contains(trimmed)) {
                        cleaned.add(trimmed);
                    }
                }
            }
        }
        this.departments = cleaned;
        this.updatedAt = clock.instant();
    }

    public void addDepartment(String department, Clock clock) {
        if (department != null && !department.isBlank()) {
            String trimmed = department.trim();
            if (!this.departments.contains(trimmed)) {
                this.departments.add(trimmed);
                this.updatedAt = clock.instant();
            }
        }
    }

    public void removeDepartment(String department, Clock clock) {
        if (department != null) {
            this.departments.removeIf(d -> d.equalsIgnoreCase(department.trim()));
            this.updatedAt = clock.instant();
        }
    }
}
