package com.prdv.schedule.domain.model;

/** Types de créneaux / motifs (module 4.1). */
public enum AppointmentType {
    FIRST_VISIT("Premiere consultation"),
    FOLLOW_UP("Consultation de suivi"),
    URGENT("Urgence"),
    TELECONSULTATION("Teleconsultation"),
    TECHNICAL_ACT("Acte technique");

    private final String label;

    AppointmentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
