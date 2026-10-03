package com.prdv.rdv.iam.domain.model.verification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Lien entre un praticien et un etablissement.
 *
 * <p>Un meme praticien peut avoir plusieurs rattachements (multi-cabinets) ;
 * le role {@link MemberRole#REPLACER} modelise la gestion des remplaçants,
 * avec une plage de validite. Le rattachement est accepte par l'etablissement.
 */
@Getter
@Setter
public class EstablishmentMembership {

    public enum MemberRole {
        OWNER,
        EMPLOYEE,
        REPLACER;

        /**
         * Deserialisation tolerante : accepte les valeurs canoniques
         * ({@code OWNER}, {@code EMPLOYEE}, {@code REPLACER}) ainsi que leurs
         * alias usuels en francais et en anglais (sans distinction de casse ni
         * d'accents). Une chaine vide retourne {@code null} afin de laisser le
         * role par defaut ({@link #EMPLOYEE}) s'appliquer.
         */
        @JsonCreator
        public static MemberRole from(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            switch (normalized) {
                case "OWNER":
                case "TITULAIRE":
                case "GERANT":
                case "MANAGER":
                case "PROPRIETAIRE":
                case "FONDATEUR":
                case "DIRECTOR":
                case "DIRECTEUR":
                case "ADMIN":
                case "CHEF":
                    return OWNER;
                case "EMPLOYEE":
                case "SALARIE":
                case "COLLABORATOR":
                case "COLLABORATEUR":
                case "ASSOCIATE":
                case "ASSOCIE":
                case "PARTNER":
                case "PARTENAIRE":
                case "MEMBER":
                case "MEMBRE":
                case "PRACTITIONER":
                case "PRATICIEN":
                case "DOCTOR":
                case "MEDECIN":
                case "STAFF":
                case "INTERNE":
                case "ASSISTANT":
                    return EMPLOYEE;
                case "REPLACER":
                case "REPLACEMENT":
                case "REMPLACANT":
                case "SUBSTITUTE":
                case "LOCUM":
                case "INTERIM":
                case "INTERIMAIRE":
                    return REPLACER;
                default:
                    throw new IllegalArgumentException("Valeur de role invalide : \"" + value
                            + "\" (valeurs acceptees : OWNER, EMPLOYEE, REPLACER)");
            }
        }
    }

    public enum MembershipStatus {
        PENDING,
        ACTIVE,
        REJECTED,
        REVOKED
    }

    private Long id;
    private Long establishmentUserId;
    private Long practitionerUserId;
    private MemberRole memberRole;
    private MembershipStatus status;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Instant requestedAt;
    private Instant decidedAt;

    public static EstablishmentMembership request(Long establishmentUserId, Long practitionerUserId,
                                                  MemberRole role, LocalDate validFrom, LocalDate validUntil,
                                                  Clock clock) {
        validateValidityPeriod(validFrom, validUntil);
        EstablishmentMembership m = new EstablishmentMembership();
        m.establishmentUserId = establishmentUserId;
        m.practitionerUserId = practitionerUserId;
        m.memberRole = role != null ? role : MemberRole.EMPLOYEE;
        m.status = MembershipStatus.PENDING;
        m.validFrom = validFrom;
        m.validUntil = validUntil;
        m.requestedAt = clock.instant();
        return m;
    }

    public void updatePending(MemberRole role, LocalDate validFrom, LocalDate validUntil, Clock clock) {
        validateValidityPeriod(validFrom, validUntil);
        if (role != null) {
            this.memberRole = role;
        } else if (this.memberRole == null) {
            this.memberRole = MemberRole.EMPLOYEE;
        }
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.requestedAt = clock.instant();
    }

    public static void validateValidityPeriod(LocalDate validFrom, LocalDate validUntil) {
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "La date de fin (validUntil) ne peut pas preceder la date de debut (validFrom)");
        }
    }

    public void accept(Clock clock) {
        this.status = MembershipStatus.ACTIVE;
        this.decidedAt = clock.instant();
    }

    public void reject(Clock clock) {
        this.status = MembershipStatus.REJECTED;
        this.decidedAt = clock.instant();
    }

    public void revoke(Clock clock) {
        this.status = MembershipStatus.REVOKED;
        this.decidedAt = clock.instant();
    }
}
