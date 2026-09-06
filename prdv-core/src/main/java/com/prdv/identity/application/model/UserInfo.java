package com.prdv.identity.application.model;

import com.prdv.identity.domain.model.Role;
import com.prdv.identity.domain.model.User;
import com.prdv.identity.domain.model.UserStatus;

/** Resultat applicatif minimal expose aux adaptateurs entree. */
public record UserInfo(Long id, String email, Role role, UserStatus status) {

    public static UserInfo of(User user) {
        return new UserInfo(user.id(), user.email(), user.role(), user.status());
    }
}
