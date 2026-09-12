package com.prdv.rdv.iam.adapter.in.security;

/**
 * Principal Spring Security : identite minimale extraite du JWT puis enrichie
 * des autorites resolues en base (revocation instantanee).
 */
public record AuthenticatedUser(Long userId, String email) {
}
