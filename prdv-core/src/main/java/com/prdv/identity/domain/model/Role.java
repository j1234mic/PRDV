package com.prdv.identity.domain.model;

/** Roles de la plateforme (RBAC - module 1.2). Un role porte une permission d'acces, rien d'autre. */
public enum Role {
    PATIENT,
    DOCTOR,
    SECRETARY,
    ADMIN;

    public static Role from(String raw) {
        return Role.valueOf(raw.trim().toUpperCase());
    }
}
