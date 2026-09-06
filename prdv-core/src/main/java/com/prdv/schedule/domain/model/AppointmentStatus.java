package com.prdv.schedule.domain.model;

/** Machine a etats du RDV (module 4.2) : PENDING -> CONFIRMED -> COMPLETED | NO_SHOW, annulation possible avant le debut. */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED_BY_PATIENT,
    CANCELLED_BY_DOCTOR,
    COMPLETED,
    NO_SHOW;

    public boolean occupiesSlot() {
        return this == PENDING || this == CONFIRMED;
    }
}
