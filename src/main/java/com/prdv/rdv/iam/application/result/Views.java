package com.prdv.rdv.iam.application.result;

import com.prdv.rdv.iam.domain.model.auth.BiometricCredential;
import com.prdv.rdv.iam.domain.model.security.BugBountyReport;
import com.prdv.rdv.iam.domain.model.security.PentestReport;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.AdminType;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.GroupInvoice;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        public boolean minor() {
            return guardianUserId != null;
        }
    }

    public record PatientKycStatusView(Long userId,
                                       PatientProfile.KycLevel kycLevel,
                                       boolean emailVerified,
                                       boolean phoneVerified,
                                       boolean identityDocumentVerified,
                                       String identityDocumentToken,
                                       boolean paymentEligible,
                                       List<String> missingRequirements) {
    }

    public record PractitionerView(UserView user, String firstName, String lastName, String specialty,
                                   String rppsNumber, String adeliNumber,
                                   boolean rppsVerified, boolean adeliVerified, boolean diplomaVerified,
                                   boolean professionalInsuranceVerified, boolean bankAccountVerified,
                                   String maskedIban, boolean applicationComplete) {
    }

    public record PractitionerInsuranceView(Long practitionerUserId,
                                            boolean verified,
                                            String insuranceCompany,
                                            String insurancePolicyNumber,
                                            LocalDate insuranceExpiresAt) {
    }

    public record SecretaryView(UserView user, String firstName, String lastName,
                                List<Long> supervisedPractitionerIds) {
    }

    public record SecretaryDetailView(UserView user,
                                      String firstName,
                                      String lastName,
                                      List<Long> supervisedPractitionerIds,
                                      List<Long> accessibleEstablishmentIds,
                                      List<String> directPermissions) {
    }

    public record EstablishmentView(UserView user, String legalName, String siret, String address,
                                    List<String> departments) {
    }

    public record GroupInvoiceView(Long id,
                                   Long establishmentUserId,
                                   String invoiceNumber,
                                   LocalDate periodStart,
                                   LocalDate periodEnd,
                                   String department,
                                   List<Long> practitionerUserIds,
                                   int consultationsCount,
                                   long totalAmountCents,
                                   long platformFeeCents,
                                   long netAmountCents,
                                   String currency,
                                   GroupInvoice.InvoiceStatus status,
                                   Instant createdAt,
                                   Instant paidAt) {
    }

    public record EstablishmentDashboardView(EstablishmentView establishment,
                                             int departmentsCount,
                                             int activePractitionersCount,
                                             int activeReplacersCount,
                                             int pendingMembershipsCount,
                                             int secretariesCount,
                                             int totalInvoicesCount,
                                             long totalBilledCents,
                                             long totalPaidCents,
                                             long pendingPaymentCents) {
    }

    public record SessionView(Long id, String ipAddress, String userAgent, String deviceLabel,
                              String country, String city, boolean suspicious, boolean current,
                              Instant createdAt, Instant lastSeenAt) {
    }

    public record BiometricCredentialView(Long id,
                                          Long userId,
                                          String credentialId,
                                          BiometricCredential.BiometricModality modality,
                                          String deviceName,
                                          long signCount,
                                          boolean revoked,
                                          Instant createdAt,
                                          Instant lastUsedAt) {
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

    /** Statistiques agregees : aucune donnee personnelle n'est exposee. */
    public record AnalyticsSummary(long totalUsers,
                                   Map<ProfileType, Long> byProfileType,
                                   Map<AccountStatus, Long> byStatus) {
        public AnalyticsSummary {
            byProfileType = Map.copyOf(byProfileType);
            byStatus = Map.copyOf(byStatus);
        }
    }

    public record E2eePublicKeyView(Long id,
                                    Long userId,
                                    String keyId,
                                    String algorithm,
                                    String publicKeyPem,
                                    String fingerprint,
                                    boolean active,
                                    Instant createdAt) {
    }

    public record E2eeEnvelopeView(Long senderUserId,
                                   Long recipientUserId,
                                   String recipientKeyId,
                                   String algorithm,
                                   String fingerprint,
                                   String cipherText,
                                   Instant sealedAt) {
    }

    public record PentestReportView(Long id,
                                    String campaignName,
                                    String auditor,
                                    String scope,
                                    int findingsCount,
                                    int criticalCount,
                                    int highCount,
                                    int mediumCount,
                                    int lowCount,
                                    int securityScore,
                                    PentestReport.Status status,
                                    String summary,
                                    List<String> findings,
                                    Instant executedAt,
                                    Instant nextScheduledAt) {
    }

    public record BugBountyReportView(Long id,
                                      String reporterEmail,
                                      String reporterAlias,
                                      String title,
                                      String description,
                                      String affectedComponent,
                                      BugBountyReport.Severity severity,
                                      Double cvssScore,
                                      BugBountyReport.Status status,
                                      long bountyAmountCents,
                                      String resolutionNote,
                                      Instant submittedAt,
                                      Instant resolvedAt) {
    }

    public record BugBountyPolicyView(String programName,
                                      String securityContact,
                                      String policyUrl,
                                      boolean safeHarborEnabled,
                                      List<String> inScopeModules,
                                      Map<String, String> rewardsBySeverity) {
    }

    public record WafEventView(String ipAddress,
                                String method,
                                String uri,
                                String matchedRule,
                                Instant occurredAt) {
    }

    public record WafDdosStatusView(boolean wafEnabled,
                                    int activeWafRulesCount,
                                    long blockedWafRequestsCount,
                                    List<WafEventView> recentWafEvents,
                                    List<String> blockedIpAddresses) {
        /** Alias conserve pour les appelants historiques du statut WAF. */
        public List<String> blockedIps() {
            return blockedIpAddresses;
        }
    }

    public record ImportSummary(String sourcePlatform, int fieldsUpdated, List<String> updatedFields) {
    }

    public record OtpSentView(String target, String channel, String purpose, long expiresInSeconds) {
    }

    public record PagedResult<T>(List<T> items, long total, int page, int size) {
    }
}
