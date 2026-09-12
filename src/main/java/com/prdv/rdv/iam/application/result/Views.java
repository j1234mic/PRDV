package com.prdv.rdv.iam.application.result;

import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.AdminType;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Vues de lecture (read models) : les adapteurs web ne manipulent jamais
 * directement les agregats du domaine.
 */
public final class Views {

    private Views() {
    }

    public record UserView(Long id, String email, String phone, ProfileType profileType,
                           AccountStatus status, AdminType adminType,
                           boolean emailVerified, boolean phoneVerified, boolean mfaEnabled,
                           List<String> roles, Instant createdAt) {
    }

    public record PatientView(UserView user, String firstName, String lastName, LocalDate birthDate,
                              PatientProfile.Gender gender, Long guardianUserId,
                              PatientProfile.KycLevel kycLevel, String importedFrom) {
    }

    public record PractitionerView(UserView user, String firstName, String lastName, String specialty,
                                   String rppsNumber, String adeliNumber,
                                   boolean rppsVerified, boolean adeliVerified, boolean diplomaVerified,
                                   boolean professionalInsuranceVerified, boolean bankAccountVerified,
                                   String maskedIban, boolean applicationComplete) {
    }

    public record SecretaryView(UserView user, String firstName, String lastName,
                                List<Long> supervisedPractitionerIds) {
    }

    public record EstablishmentView(UserView user, String legalName, String siret, String address,
                                    List<String> departments) {
    }

    public record SessionView(Long id, String ipAddress, String userAgent, String deviceLabel,
                              String country, String city, boolean suspicious, boolean current,
                              Instant createdAt, Instant lastSeenAt) {
    }

    public record KycDocumentView(Long id, Long ownerUserId, KycDocument.DocumentType type,
                                  String originalFilename, String contentType, long sizeBytes,
                                  KycDocument.ReviewStatus status, String reviewNote,
                                  Instant uploadedAt, Instant reviewedAt) {
    }

    public record MembershipView(Long id, Long establishmentUserId, Long practitionerUserId,
                                 EstablishmentMembership.MemberRole role,
                                 EstablishmentMembership.MembershipStatus status,
                                 LocalDate validFrom, LocalDate validUntil, Instant requestedAt) {
    }

    public record ContractView(Long id, Long practitionerUserId, String version,
                               String contentHash, String ipAddress, Instant acceptedAt) {
    }

    public record DelegationView(Long id, Long granterUserId, Long granteeUserId,
                                 List<String> permissionCodes, String reason,
                                 Instant validFrom, Instant validUntil, Instant revokedAt) {
    }

    public record AuditView(Long id, Long userId, String action, String outcome,
                            String resourceType, String resourceId, String detail,
                            String ipAddress, Instant createdAt) {
    }

    public record RoleView(Long id, String name, String description, boolean system,
                           List<String> permissionCodes) {
    }

    public record PermissionView(String code, String module, String description) {
    }

    /** Vue anonymisee pour les analystes de donnees (aucune donnee personnelle directe). */
    public record AnonymizedUserView(String pseudonym, ProfileType profileType,
                                     String kycLevel, Integer ageRangeMin, Integer ageRangeMax,
                                     int accountAgeYears) {
    }

    public record ImportSummary(String sourcePlatform, int fieldsUpdated, List<String> updatedFields) {
    }

    public record OtpSentView(String target, String channel, String purpose, long expiresInSeconds) {
    }

    public record PagedResult<T>(List<T> items, long total, int page, int size) {
    }
}
