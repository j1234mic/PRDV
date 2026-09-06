package com.prdv.profile.domain.model;

import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.DomainException;
import com.prdv.shared.exception.ValidationException;

import java.util.List;

/**
 * Profil praticien (module 2.2) - AGREGAT avec workflow de verification :
 * soumission -> controle automatique RPPS (algorithme + annuaire) -> validation manuelle admin.
 * La regle "un profil VERIFIE est requis pour exercer" vit ici, pas dans les services.
 */
public final class DoctorProfile {

    private Long id;
    private final Long userId;
    private String fullName;
    private RppsNumber rpps;
    private String specialty;
    private List<String> subSpecialties;
    private String description;
    private int sector;
    private int consultationFeeCents;
    private List<String> languages;
    private List<PracticeLocation> practiceLocations;
    private DoctorVerificationStatus verification;
    private String rejectionReason;

    public DoctorProfile(Long id, Long userId, String fullName, RppsNumber rpps, String specialty,
                         List<String> subSpecialties, String description, int sector, int consultationFeeCents,
                         List<String> languages, List<PracticeLocation> practiceLocations,
                         DoctorVerificationStatus verification, String rejectionReason) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.rpps = rpps;
        this.specialty = specialty;
        this.subSpecialties = subSpecialties == null ? List.of() : List.copyOf(subSpecialties);
        this.description = description;
        this.sector = sector;
        this.consultationFeeCents = consultationFeeCents;
        this.languages = languages == null ? List.of("fr") : List.copyOf(languages);
        this.practiceLocations = practiceLocations == null ? List.of() : List.copyOf(practiceLocations);
        this.verification = verification;
        this.rejectionReason = rejectionReason;
        validate();
    }

    private void validate() {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Nom du praticien requis");
        }
        if (specialty == null || specialty.isBlank()) {
            throw new ValidationException("Specialite requise");
        }
        if (sector < 1 || sector > 3) {
            throw new ValidationException("Secteur de convention invalide (1, 2 ou 3)");
        }
        if (consultationFeeCents <= 0) {
            throw new ValidationException("Tarif de consultation requis");
        }
    }

    /** Usine : premier enregistrement -> passe en verification. */
    public static DoctorProfile submitForValidation(Long userId, String fullName, RppsNumber rpps, String specialty) {
        DoctorProfile p = new DoctorProfile(null, userId, fullName, rpps, specialty, List.of(), null,
                1, 2500, List.of("fr"), List.of(), DoctorVerificationStatus.PENDING_VALIDATION, null);
        return p;
    }

    public void approve() {
        if (verification != DoctorVerificationStatus.PENDING_VALIDATION
                && verification != DoctorVerificationStatus.REJECTED) {
            throw new ConflictException("Seul un profil en attente peut etre valide");
        }
        this.verification = DoctorVerificationStatus.VERIFIED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        if (verification == DoctorVerificationStatus.VERIFIED) {
            throw new ConflictException("Un profil valide ne se rejette pas : utiliser la suspension de compte");
        }
        this.verification = DoctorVerificationStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void ensureVerified() {
        if (verification != DoctorVerificationStatus.VERIFIED) {
            throw new DomainException("Profil non verifie par l'ordre : exercice interdit sur la plateforme");
        }
    }

    /** Edition complete (le compte utilisateur reste le meme ; re-soumission si modification majeure). */
    public void edit(String fullName, String specialty, String description, int sector, int feeCents,
                     List<String> languages, List<PracticeLocation> locations) {
        DoctorProfile edited = new DoctorProfile(id, userId, fullName, rpps, specialty, subSpecialties,
                description, sector, feeCents, languages, locations, verification, rejectionReason);
        this.fullName = edited.fullName;
        this.specialty = edited.specialty;
        this.description = edited.description;
        this.sector = edited.sector;
        this.consultationFeeCents = edited.consultationFeeCents;
        this.languages = edited.languages;
        this.practiceLocations = edited.practiceLocations;
        this.subSpecialties = edited.subSpecialties;
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Identifiant deja affecte");
        }
        this.id = id;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public String fullName() { return fullName; }
    public RppsNumber rpps() { return rpps; }
    public String specialty() { return specialty; }
    public List<String> subSpecialties() { return subSpecialties; }
    public String description() { return description; }
    public int sector() { return sector; }
    public int consultationFeeCents() { return consultationFeeCents; }
    public List<String> languages() { return languages; }
    public List<PracticeLocation> practiceLocations() { return practiceLocations; }
    public DoctorVerificationStatus verification() { return verification; }
    public String rejectionReason() { return rejectionReason; }
}
