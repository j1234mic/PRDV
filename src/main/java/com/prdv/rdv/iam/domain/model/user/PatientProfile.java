package com.prdv.rdv.iam.domain.model.user;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Profil patient. Gere egalement le rattachement d'un mineur a un parent/tuteur
 * et le niveau KYC (Know Your Customer) necessaire aux paiements.
 */
@Getter
@Setter
public class PatientProfile {

    public enum Gender { M, F, OTHER }

    public enum KycLevel {
        /** Aucune verification. */
        NONE,
        /** Email et telephone verifies. */
        BASIC,
        /** Piece d'identite verifiee (CNI / passeport) : eligible au paiement. */
        VERIFIED
    }

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Gender gender;

    /** Patient mineur : id de l'utilisateur parent/tuteur rattache. */
    private Long guardianUserId;

    /** Reference (jeton) du document d'identite : la valeur brute n'est jamais stockee. */
    private String identityDocumentToken;

    private KycLevel kycLevel = KycLevel.NONE;

    /** Plateforme d'origine lors d'un import de donnees. */
    private String importedFrom;

    private Instant createdAt;
    private Instant updatedAt;

    public static PatientProfile create(Long userId, String firstName, String lastName,
                                        LocalDate birthDate, Gender gender,
                                        Long guardianUserId, Clock clock) {
        PatientProfile p = new PatientProfile();
        p.userId = userId;
        p.firstName = firstName;
        p.lastName = lastName;
        p.birthDate = birthDate;
        p.gender = gender;
        p.guardianUserId = guardianUserId;
        p.createdAt = clock.instant();
        p.updatedAt = p.createdAt;
        return p;
    }

    public boolean isMinor(Clock clock) {
        return birthDate != null && birthDate.isAfter(LocalDate.now(clock).minusYears(18));
    }

    public void upgradeKyc(KycLevel newLevel, Clock clock) {
        if (newLevel.ordinal() > this.kycLevel.ordinal()) {
            this.kycLevel = newLevel;
            this.updatedAt = clock.instant();
        }
    }
}
