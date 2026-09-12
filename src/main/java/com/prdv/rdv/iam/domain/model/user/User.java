package com.prdv.rdv.iam.domain.model.user;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Aggregat racine « Utilisateur ».
 *
 * <p>Il porte le cycle de vie du compte, la securite de connexion
 * (tentatives, verrouillage, version de tokens pour revocation instantanee)
 * et la liste des roles. Les comportements de securite sont encapsules ici
 * (modele riche) plutot que disperses dans les services.
 */
@Getter
@Setter
public class User {

    private Long id;
    private String email;
    private String phone;
    private String passwordHash;
    private ProfileType profileType;
    private AccountStatus status;
    private AdminType adminType;

    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean mfaEnabled;

    /** Secret TOTP chiffre (AES-GCM via un converter JPA) ; null si MFA inactive. */
    private String totpSecret;

    /** Secret TOTP en attente de confirmation entre « beginSetup » et « enable ». */
    private String pendingTotpSecret;

    private int failedAttempts;
    private Instant lockedUntil;

    /** Incremente -> tous les access tokens emis deviennent invalides (revocation globale). */
    private long tokenVersion;

    /** Permissions attribuees directement (granularite fine, ex : secretaire). */
    private Set<String> directPermissions = new HashSet<>();

    private Set<Role> roles = new HashSet<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;

    // ------------------------------------------------------------------
    // Fabriques
    // ------------------------------------------------------------------

    /**
     * Inscription d'un compte dont l'email doit encore etre confirme par OTP.
     */
    public static User register(Email email, PhoneNumber phone, String passwordHash,
                                ProfileType profileType, Clock clock) {
        User user = new User();
        user.email = email.value();
        user.phone = phone == null ? null : phone.value();
        user.passwordHash = passwordHash;
        user.profileType = profileType;
        user.status = AccountStatus.PENDING_EMAIL_VERIFICATION;
        user.tokenVersion = 0;
        user.createdAt = clock.instant();
        user.updatedAt = user.createdAt;
        return user;
    }

    /** Compte cree a partir d'un fournisseur d'identite social (email deja verifie par le FAI). */
    public static User fromSocialProvider(String email, ProfileType profileType, Clock clock) {
        User user = new User();
        user.email = Email.of(email).value();
        user.profileType = profileType;
        user.status = AccountStatus.ACTIVE;
        user.emailVerified = true;
        user.createdAt = clock.instant();
        user.updatedAt = user.createdAt;
        return user;
    }

    // ------------------------------------------------------------------
    // Comportements : verification du compte
    // ------------------------------------------------------------------

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    /**
     * Le compte est pret : actif pour les patients ;
     * pour les profils controles (praticien, etablissement, secretaire),
     * il passe en attente de validation manuelle.
     */
    public void afterContactVerification(Set<ProfileType> manuallyValidatedProfiles, Clock clock) {
        if (manuallyValidatedProfiles.contains(profileType)) {
            this.status = AccountStatus.PENDING_VALIDATION;
        } else {
            this.status = AccountStatus.ACTIVE;
        }
        this.updatedAt = clock.instant();
    }

    // ------------------------------------------------------------------
    // Comportements : securite de connexion
    // ------------------------------------------------------------------

    /**
     * Verrouillage temporaire automatique : on se fonde sur l'horodatage
     * (le statut LOCKED est leve paresseusement a la prochaine connexion reussie).
     */
    public boolean isCurrentlyLocked(Clock clock) {
        return lockedUntil != null && lockedUntil.isAfter(clock.instant());
    }

    /** Enregistre un echec de connexion et verrouille le compte au-dela du seuil. */
    public void recordFailedLogin(int maxAttempts, Duration lockDuration, Clock clock) {
        this.failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            this.status = AccountStatus.LOCKED;
            this.lockedUntil = clock.instant().plus(lockDuration);
            this.failedAttempts = 0;
        }
        this.updatedAt = clock.instant();
    }

    public void resetFailedLogins(Clock clock) {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        if (this.status == AccountStatus.LOCKED) {
            this.status = AccountStatus.ACTIVE;
        }
        this.lastLoginAt = clock.instant();
        this.updatedAt = clock.instant();
    }

    public void activate(Clock clock) {
        this.status = AccountStatus.ACTIVE;
        this.lockedUntil = null;
        this.updatedAt = clock.instant();
    }

    public void reject(Clock clock) {
        this.status = AccountStatus.REJECTED;
        this.updatedAt = clock.instant();
    }

    public void suspend(Clock clock) {
        this.status = AccountStatus.SUSPENDED;
        this.updatedAt = clock.instant();
    }

    public void storePendingMfaSecret(String pendingTotpSecret, Clock clock) {
        this.pendingTotpSecret = pendingTotpSecret;
        this.updatedAt = clock.instant();
    }

    public void enableMfa(Clock clock) {
        this.totpSecret = pendingTotpSecret;
        this.pendingTotpSecret = null;
        this.mfaEnabled = true;
        this.updatedAt = clock.instant();
    }

    public void disableMfa(Clock clock) {
        this.totpSecret = null;
        this.pendingTotpSecret = null;
        this.mfaEnabled = false;
        this.updatedAt = clock.instant();
    }

    /** Invalidation de toutes les sessions (mot de passe oublie, suspicion de fraude...). */
    public void revokeAllTokens(Clock clock) {
        this.tokenVersion++;
        this.updatedAt = clock.instant();
    }

    /**
     * Droit a l'oubli (RGPD) : anonymisation irreversible du compte.
     */
    public void anonymize(Clock clock) {
        this.email = "deleted+" + id + "@anonymized.prdv";
        this.phone = null;
        this.passwordHash = null;
        this.totpSecret = null;
        this.pendingTotpSecret = null;
        this.mfaEnabled = false;
        this.directPermissions.clear();
        this.roles.clear();
        this.status = AccountStatus.DELETED;
        this.updatedAt = clock.instant();
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void assertCanAuthenticate(Clock clock) {
        if (status == AccountStatus.DELETED) {
            throw IamException.of(IamErrorCode.INVALID_CREDENTIALS, "Compte inexistant");
        }
        if (isCurrentlyLocked(clock)) {
            throw IamException.of(IamErrorCode.ACCOUNT_LOCKED,
                    "Compte verrouille jusqu'a " + lockedUntil + " suite a des echecs de connexion");
        }
        if (status == AccountStatus.SUSPENDED || status == AccountStatus.REJECTED) {
            throw IamException.of(IamErrorCode.ACCOUNT_SUSPENDED, "Compte "
                    + (status == AccountStatus.REJECTED ? "refuse" : "suspendu") + ", contactez le support");
        }
        if (status == AccountStatus.PENDING_EMAIL_VERIFICATION) {
            throw IamException.of(IamErrorCode.EMAIL_NOT_VERIFIED,
                    "Veuillez confirmer votre email avec le code OTP recu");
        }
    }
}
