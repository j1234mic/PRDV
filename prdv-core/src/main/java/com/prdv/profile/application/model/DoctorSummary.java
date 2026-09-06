package com.prdv.profile.application.model;

/** Projection "carte de visite" utilisee par l'annuaire et la prise de RDV (DDD read model). */
public record DoctorSummary(Long userId, String fullName, String specialty, int sector,
                            int consultationFeeCents, String firstCity, String avatarUrl) {
}
