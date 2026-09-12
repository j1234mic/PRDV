package com.prdv.rdv.iam.domain.model.rbac;

import lombok.Getter;
import lombok.Setter;

/**
 * Permission atomique : code de la forme {@code module:action}
 * (ex : {@code iam.role.write}, {@code appointment.cancel}).
 * C'est la granularite minimale du systeme RBAC.
 */
@Getter
@Setter
public class Permission {

    private Long id;
    private String code;
    private String module;
    private String description;

    public static Permission create(String code, String module, String description) {
        Permission p = new Permission();
        p.code = code;
        p.module = module;
        p.description = description;
        return p;
    }
}
