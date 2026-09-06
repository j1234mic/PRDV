package com.prdv.identity.domain.model;

import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.DomainException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * AGREGAT RACINE (module 1) : le compte utilisateur, independant du profil metier
 * (patient/medecin) qui lui est rattache par userId.
 *
 * La machine a etats du compte est PORTee par l'entite (pas par le service) :
 * les regles "compte verifie obligatoire", "verrouillage apres N echecs" sont
 * invariants du domaine -> Single Responsibility du service d'application preserve.
 */
public final class User {

    private Long id;
    private final String email;
    private final String phone;
    private PasswordHash passwordHash;
    private final Role role;
    private UserStatus status;
    private boolean emailVerified;
    private int failedAttempts;
    private LocalDateTime lockedUntil;
    private final LocalDateTime createdAt;

    private User(Long id, String email, String phone, PasswordHash passwordHash, Role role,
                 UserStatus status, boolean emailVerified, int failedAttempts,
                 LocalDateTime lockedUntil, LocalDateTime createdAt) {
        this.id = id;
        this.email = email.toLowerCase();
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
    }

    /** Pattern FACTORY : construction d'un nouvel inscrit dans son etat initial. */
    public static User register(String email, String phone, PasswordHash passwordHash, Role role,
                                LocalDateTime now) {
        if (email == null || !email.contains("@")) {
            throw new DomainException("Email invalide");
        }
        return new User(null, email, phone, passwordHash, role,
                UserStatus.PENDING_VERIFICATION, false, 0, null, now);
    }

    /** Reconstitution depuis la persistance (utilisé par l'adaptateur JPA). */
    public static User restore(Long id, String email, String phone, PasswordHash passwordHash, Role role,
                               UserStatus status, boolean emailVerified, int failedAttempts,
                               LocalDateTime lockedUntil, LocalDateTime createdAt) {
        return new User(id, email, phone, passwordHash, role, status, emailVerified,
                failedAttempts, lockedUntil, createdAt);
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Identifiant deja affecte");
        }
        this.id = id;
    }

    // ----- transitions d'etat -------------------------------------------------

    public void verifyEmail() {
        if (status != UserStatus.PENDING_VERIFICATION) {
            throw new ConflictException("Le compte n'attend pas de verification email");
        }
        this.emailVerified = true;
        this.status = UserStatus.ACTIVE;
    }

    /** Appelé par AccountLockoutPolicy (STRATEGY) a chaque echec d'authentification. */
    public void registerFailedAttempt(LocalDateTime now, int maxAttempts, Duration lockDuration) {
        this.failedAttempts++;
        if (this.failedAttempts >= maxAttempts) {
            this.status = UserStatus.LOCKED;
            this.lockedUntil = now.plus(lockDuration);
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    /** Deverrouille automatiquement si le delai de blocage est ecoule. */
    public void unlockIfExpired(LocalDateTime now) {
        if (status == UserStatus.LOCKED && lockedUntil != null && !now.isBefore(lockedUntil)) {
            this.status = UserStatus.ACTIVE;
            this.failedAttempts = 0;
            this.lockedUntil = null;
        }
    }

    public void ensureCanAuthenticate(LocalDateTime now) {
        unlockIfExpired(now);
        switch (status) {
            case PENDING_VERIFICATION -> throw new DomainException("Compte non verifie : saisissez le code OTP recu par email");
            case LOCKED -> throw new DomainException("Compte temporairement bloque suite a des tentatives repetees");
            case SUSPENDED -> throw new DomainException("Compte suspendu, contactez le support");
            case ACTIVE -> { }
        }
    }

    // ----- getters ------------------------------------------------------------

    public Long id() { return id; }
    public String email() { return email; }
    public String phone() { return phone; }
    public PasswordHash passwordHash() { return passwordHash; }
    public Role role() { return role; }
    public UserStatus status() { return status; }
    public boolean emailVerified() { return emailVerified; }
    public int failedAttempts() { return failedAttempts; }
    public LocalDateTime lockedUntil() { return lockedUntil; }
    public LocalDateTime createdAt() { return createdAt; }
}
