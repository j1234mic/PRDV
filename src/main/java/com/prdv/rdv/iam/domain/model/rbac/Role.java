package com.prdv.rdv.iam.domain.model.rbac;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Role RBAC : ensemble de permissions. Les roles predefinis sont marques
 * {@code system = true} ; les roles personnalises peuvent etre crees par
 * les administrateurs.
 */
@Getter
@Setter
public class Role {

    public static final String PATIENT = "ROLE_PATIENT";
    public static final String PRACTITIONER = "ROLE_PRACTITIONER";
    public static final String SECRETARY = "ROLE_SECRETARY";
    public static final String ESTABLISHMENT = "ROLE_ESTABLISHMENT";
    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String MODERATOR = "ROLE_MODERATOR";
    public static final String TECHNICAL_SUPPORT = "ROLE_TECHNICAL_SUPPORT";
    public static final String COMMERCIAL_SUPPORT = "ROLE_COMMERCIAL_SUPPORT";
    public static final String DATA_ANALYST = "ROLE_DATA_ANALYST";

    private Long id;
    private String name;
    private String description;
    private boolean system;
    private Set<Permission> permissions = new HashSet<>();

    public static Role system(String name, String description, Set<Permission> permissions) {
        Role r = new Role();
        r.name = name;
        r.description = description;
        r.system = true;
        r.permissions = new HashSet<>(permissions);
        return r;
    }

    public static Role custom(String name, String description, Set<Permission> permissions) {
        Role r = new Role();
        r.name = name;
        r.description = description;
        r.system = false;
        r.permissions = new HashSet<>(permissions);
        return r;
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }
}
