package com.prdv.rdv.iam.domain.model.user;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Profil secretaire medicale : compte rattache a un ou plusieurs praticiens,
 * possiblement sur plusieurs cabinets. Les droits fins sont portes par
 * {@code User.directPermissions} (RBAC granulaire).
 */
@Getter
@Setter
public class SecretaryProfile {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;

    /** Identifiants des praticiens dont la secretaire gere les agendas. */
    private Set<Long> supervisedPractitionerIds = new HashSet<>();

    private Instant createdAt;
    private Instant updatedAt;

    public static SecretaryProfile create(Long userId, String firstName, String lastName,
                                          Set<Long> practitionerIds, Clock clock) {
        SecretaryProfile p = new SecretaryProfile();
        p.userId = userId;
        p.firstName = firstName;
        p.lastName = lastName;
        if (practitionerIds != null) {
            p.supervisedPractitionerIds = new HashSet<>(practitionerIds);
        }
        p.createdAt = clock.instant();
        p.updatedAt = p.createdAt;
        return p;
    }
}
