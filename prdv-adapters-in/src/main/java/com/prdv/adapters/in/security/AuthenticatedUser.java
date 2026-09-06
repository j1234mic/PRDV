package com.prdv.adapters.in.security;

/** Principal injecte dans les controllers (@AuthenticationPrincipal). */
public record AuthenticatedUser(Long id, String email, String role) {

    public boolean isDoctor() {
        return "DOCTOR".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
