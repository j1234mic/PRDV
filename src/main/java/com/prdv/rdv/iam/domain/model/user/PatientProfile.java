package com.prdv.rdv.iam.domain.model.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Profil patient. Gere egalement le rattachement d'un mineur a un parent/tuteur
 * et le niveau KYC (Know Your Customer) necessaire aux paiements.
 */
@Getter
@Setter
public class PatientProfile {

    public enum Gender {
        M, F, OTHER;

        /**
         * Deserialisation tolerante : accepte les valeurs canoniques (M, F, OTHER)
         * ainsi que leurs alias usuels (MALE, FEMALE, HOMME, FEMME...), sans
         * distinction de casse. La valeur canonique reste utilisee pour la
         * serialisation et la persistance.
         */
        @JsonCreator
        public static Gender from(String value) {
            if (value == null) {
                return null;
            }
            switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "M":
                case "MALE":
                case "MAN":
                case "HOMME":
                    return M;
                case "F":
                case "FEMALE":
                case "WOMAN":
                case "FEMME":
                    return F;
                case "OTHER":
                case "AUTRE":
                case "X":
                    return OTHER;
                default:
                    throw new IllegalArgumentException("Valeur de gender invalide : \"" + value
                            + "\" (valeurs acceptees : M, F, OTHER)");
            }
        }
    }

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
