package com.prdv.rdv.profile.application.result;

import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import com.prdv.rdv.profile.domain.model.practitioner.Badge;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Vues de lecture du module « profils &amp; gestion des donnees ».
 *
 * <p>Read models immuables : les entites du domaine et les entites JPA ne
 * franchissent jamais la frontiere des adapteurs web. Les donnees sensibles
 * (NIR, IBAN) n'y apparaissent que masquees.
 */
public final class ProfileViews {

    private ProfileViews() {
    }

    // ------------------------------------------------------------------
    // Identite patient
    // ------------------------------------------------------------------

    public record CivilStatusView(PatientIdentity.Civility civility,
                                  String firstName,
                                  String birthName,
                                  String lastName,
                                  String preferredName,
                                  LocalDate birthDate,
                                  String birthPlace,
                                  String birthCountry,
                                  PatientIdentity.Gender gender,
                                  PatientIdentity.MaritalStatus maritalStatus,
                                  String nationality) {
    }

    public record ContactPointView(PatientIdentity.ContactType type,
                                   String value,
                                   boolean verified,
                                   boolean preferred) {
    }

    public record PostalAddressView(PatientIdentity.AddressType type,
                                    String line1,
                                    String line2,
                                    String postalCode,
                                    String city,
                                    String country,
                                    Double latitude,
                                    Double longitude,
                                    boolean isDefault) {
    }

    public record EmergencyContactView(String firstName,
                                       String lastName,
                                       String relationship,
                                       String phone,
                                       String email) {
    }

    public record TreatingPhysicianView(String firstName,
                                        String lastName,
                                        String rppsNumber,
                                        String phone,
                                        String email,
                                        boolean declaredToInsurance) {
    }

    public record HealthInsuranceView(PatientIdentity.InsuranceType type,
                                      String organization,
                                      String memberNumber,
                                      String contractReference,
                                      LocalDate validUntil) {
    }

    public record VitaleCardView(String cardVersion,
                                 LocalDate issuedOn,
                                 LocalDate expiresOn,
                                 PatientIdentity.VitaleReadMode readMode,
                                 boolean hasScan,
                                 boolean expired,
                                 Instant readAt) {
    }

    public record DmpAccountView(String dmpIdentifier,
                                 boolean linked,
                                 boolean sharingEnabled,
                                 Instant linkedAt,
                                 Instant lastSyncAt) {
    }

    public record PatientIdentityView(Long userId,
                                      CivilStatusView civilStatus,
                                      List<ContactPointView> contacts,
                                      List<PostalAddressView> addresses,
                                      EmergencyContactView emergencyContact,
                                      TreatingPhysicianView treatingPhysician,
                                      String maskedSocialSecurityNumber,
                                      boolean hasPhoto,
                                      boolean hasIdentityDocument,
                                      VitaleCardView vitaleCard,
                                      HealthInsuranceView primaryInsurance,
                                      HealthInsuranceView complementaryInsurance,
                                      DmpAccountView dmp,
                                      Instant createdAt,
                                      Instant updatedAt) {
    }

    // ------------------------------------------------------------------
    // Dossier medical personnel
    // ------------------------------------------------------------------

    public record HistoryEntryView(String id,
                                   String code,
                                   String label,
                                   LocalDate diagnosedOn,
                                   MedicalRecord.HistoryStatus status,
                                   String note,
                                   String source) {
    }

    public record FamilyHistoryView(String id,
                                    MedicalRecord.FamilyRelation relation,
                                    String condition,
                                    Integer relativeAgeAtDiagnosis,
                                    String note) {
    }

    public record AllergyView(String id,
                              String allergen,
                              MedicalRecord.AllergenType type,
                              MedicalRecord.AllergySeverity severity,
                              String reaction,
                              LocalDate declaredOn,
                              boolean active) {
    }

    public record VaccinationView(String id,
                                  String vaccine,
                                  String dose,
                                  LocalDate administeredOn,
                                  LocalDate nextReminderOn,
                                  String provider,
                                  boolean mandatory,
                                  boolean overdue) {
    }

    public record VaccinationReminderView(String id,
                                          String vaccine,
                                          LocalDate nextReminderOn,
                                          long daysUntilReminder,
                                          boolean overdue) {
    }

    public record ChronicConditionView(String id,
                                       String code,
                                       String label,
                                       LocalDate since,
                                       boolean longTermCondition,
                                       String note) {
    }

    public record TreatmentView(String id,
                                String drug,
                                String dosage,
                                String frequency,
                                String prescriber,
                                LocalDate startedOn,
                                LocalDate endedOn,
                                boolean ongoing,
                                String note) {
    }

    public record SurgeryView(String id,
                              String procedure,
                              LocalDate performedOn,
                              String establishment,
                              String surgeon,
                              String note) {
    }

    public record HospitalizationView(String id,
                                      String reason,
                                      String establishment,
                                      LocalDate admittedOn,
                                      LocalDate dischargedOn,
                                      String note) {
    }

    public record DisabilityView(String id,
                                 MedicalRecord.DisabilityType type,
                                 Integer disabilityRatePercent,
                                 List<String> aids,
                                 String note) {
    }

    public record BodyMetricsView(BigDecimal heightCm,
                                  BigDecimal weightKg,
                                  BigDecimal bmi,
                                  String bmiCategory) {
    }

    public record VitalSignsView(Integer systolicMmHg,
                                 Integer diastolicMmHg,
                                 Integer heartRateBpm,
                                 BigDecimal temperatureCelsius,
                                 Integer oxygenSaturationPercent,
                                 Integer respiratoryRatePerMin,
                                 BodyMetricsView bodyMetrics,
                                 Instant measuredAt,
                                 boolean hypertensive,
                                 boolean fever,
                                 boolean hypoxemic) {
    }

    public record MedicalRecordView(Long patientUserId,
                                    String bloodGroup,
                                    List<HistoryEntryView> history,
                                    List<FamilyHistoryView> familyHistory,
                                    List<AllergyView> allergies,
                                    List<VaccinationView> vaccinations,
                                    List<ChronicConditionView> chronicConditions,
                                    List<TreatmentView> treatments,
                                    List<SurgeryView> surgeries,
                                    List<HospitalizationView> hospitalizations,
                                    List<DisabilityView> disabilities,
                                    VitalSignsView lastVitalSigns,
                                    List<VaccinationReminderView> upcomingReminders,
                                    Instant updatedAt) {
    }

    // ------------------------------------------------------------------
    // Documents medicaux
    // ------------------------------------------------------------------

    public record DocumentVersionView(int version,
                                      String originalFilename,
                                      String contentType,
                                      long sizeBytes,
                                      String sha256,
                                      String changeNote,
                                      Long uploadedBy,
                                      Instant uploadedAt) {
    }

    public record DocumentShareView(String id,
                                    Long granteeUserId,
                                    MedicalDocument.SharePermission permission,
                                    String reason,
                                    Instant grantedAt,
                                    Instant expiresAt,
                                    Instant revokedAt,
                                    boolean active) {
    }

    public record OcrResultView(MedicalDocument.OcrStatus status,
                                Map<String, String> extractedFields,
                                double confidence,
                                String engine,
                                Instant processedAt) {
    }

    public record MedicalDocumentView(Long id,
                                      Long ownerUserId,
                                      String title,
                                      MedicalDocument.DocumentCategory category,
                                      MedicalDocument.DocumentStatus status,
                                      MedicalDocument.ClassificationSource classificationSource,
                                      Double classificationConfidence,
                                      OcrResultView ocr,
                                      String dicomModality,
                                      String dicomStudyDescription,
                                      int currentVersion,
                                      String originalFilename,
                                      String contentType,
                                      long sizeBytes,
                                      String dmpReference,
                                      Instant dmpSharedAt,
                                      List<DocumentVersionView> versions,
                                      List<DocumentShareView> shares,
                                      Instant createdAt,
                                      Instant updatedAt) {
    }

    /** Contenu binaire d'un document, transporte jusqu'a l'adapteur REST. */
    public record DocumentFile(String filename, String contentType, long sizeBytes, byte[] content) {
    }

    // ------------------------------------------------------------------
    // Sante connectee
    // ------------------------------------------------------------------

    public record ConnectedDeviceView(String id,
                                      ConnectedDevice.DeviceType type,
                                      ConnectedDevice.Provider provider,
                                      String label,
                                      String model,
                                      ConnectedDevice.Status status,
                                      String lastSyncError,
                                      Instant connectedAt,
                                      Instant lastSyncAt,
                                      long syncedMetricsCount) {
    }

    public record HealthMetricView(String id,
                                   HealthMetric.MetricType type,
                                   BigDecimal value,
                                   String unit,
                                   HealthMetric.MeasurementContext context,
                                   Instant recordedAt,
                                   String deviceId,
                                   String sourceLabel) {
    }

    public record MetricPointView(Instant bucketStart,
                                  BigDecimal average,
                                  BigDecimal min,
                                  BigDecimal max,
                                  int count) {
    }

    public record MetricStatsView(BigDecimal min, BigDecimal max, BigDecimal average, int count) {
    }

    /** Serie temporelle prete a etre tracee (graphique d'evolution). */
    public record MetricSeriesView(HealthMetric.MetricType type,
                                   String unit,
                                   String bucket,
                                   List<MetricPointView> points,
                                   MetricStatsView stats) {
    }

    public record HealthAlertView(String id,
                                  HealthMetric.MetricType metricType,
                                  BigDecimal observedValue,
                                  BigDecimal threshold,
                                  HealthAlert.Severity severity,
                                  String message,
                                  Instant triggeredAt,
                                  boolean acknowledged) {
    }

    public record SyncReportView(String deviceId, int importedMetrics, int rejectedMetrics,
                                 List<HealthAlertView> alerts) {
    }

    // ------------------------------------------------------------------
    // Preferences & confidentialite
    // ------------------------------------------------------------------

    public record AccessibilityView(List<String> needs,
                                    boolean screenReaderOptimized,
                                    boolean largePrint,
                                    String signLanguage,
                                    boolean subtitlesRequired) {
    }

    public record CommunicationView(List<String> preferredChannels,
                                    boolean quietHoursEnabled,
                                    LocalTime quietHoursStart,
                                    LocalTime quietHoursEnd) {
    }

    /** Consentement RGPD : finalite, etat courant et preuve (version + horodatage). */
    public record ActiveConsentView(String purpose,
                                    boolean granted,
                                    String policyVersion,
                                    Instant recordedAt,
                                    Instant withdrawnAt) {
    }

    public record VisibilityRuleView(String category, String level, List<Long> granteeUserIds) {
    }

    public record PrivacyPreferencesView(Long userId,
                                         List<String> preferredLanguages,
                                         String primaryLanguage,
                                         AccessibilityView accessibility,
                                         CommunicationView communication,
                                         List<ActiveConsentView> consents,
                                         List<VisibilityRuleView> visibilityRules,
                                         boolean dmpSharingEnabled,
                                         Instant erasureRequestedAt,
                                         Instant updatedAt) {
    }

    /** Export de portabilite RGPD : le contenu est genere par un adapteur (JSON, CSV...). */
    public record DataExportView(String filename, String format, long sizeBytes, String content) {
    }

    // ------------------------------------------------------------------
    // Profil praticien
    // ------------------------------------------------------------------

    public record DiplomaView(String label, String institution, Integer year, String specialty) {
    }

    public record AccreditationView(String organism, String reference, LocalDate validUntil) {
    }

    public record TariffView(String actType,
                             BigDecimal amount,
                             String currency,
                             boolean coveredByInsurance,
                             String note) {
    }

    public record AgeRangeView(Integer minAge,
                               Integer maxAge,
                               boolean acceptsChildren,
                               boolean acceptsAdults,
                               boolean acceptsSeniors) {
    }

    public record PractitionerIdentityView(PractitionerDossier.Title title,
                                           String firstName,
                                           String lastName,
                                           String rppsNumber,
                                           String adeliNumber,
                                           PractitionerDossier.RegistrationOrder registrationOrder,
                                           String mainSpecialty,
                                           List<String> secondarySpecialties,
                                           List<String> subSpecialties,
                                           List<String> skills,
                                           List<DiplomaView> diplomas,
                                           Integer experienceYears,
                                           List<String> languages,
                                           boolean hasPhoto,
                                           boolean hasPresentationVideo,
                                           String shortBio) {
    }

    public record PracticeInformationView(PractitionerDossier.ConventionSector sector,
                                          List<TariffView> tariffs,
                                          boolean optam,
                                          boolean optamCo,
                                          LocalDate optamSignedOn,
                                          List<String> paymentMethods,
                                          boolean thirdPartyPayment,
                                          String thirdPartyPaymentConditions,
                                          List<String> actsPerformed,
                                          List<String> availableEquipment,
                                          List<String> treatedPathologies,
                                          AgeRangeView acceptedAges,
                                          boolean teleconsultation,
                                          String teleconsultationPlatform) {
    }

    public record ManagementView(String siret,
                                 String maskedIban,
                                 String professionalInsurer,
                                 String insurancePolicyNumber,
                                 LocalDate insuranceExpiresOn,
                                 String urssafNumber,
                                 String accountingAssociation,
                                 String accountingAssociationNumber,
                                 Integer ordinalCotisationYear,
                                 BigDecimal ordinalCotisationAmount,
                                 List<String> qualityCertifications,
                                 List<AccreditationView> accreditations) {
    }

    public record VisibilityProfileView(String longDescription,
                                        List<String> expertiseDomains,
                                        List<String> publications,
                                        List<String> distinctions,
                                        List<String> conferenceTalks,
                                        List<String> mediaReferences) {
    }

    public record NetworkContactView(String id,
                                     PractitionerDossier.NetworkRole role,
                                     String name,
                                     String specialty,
                                     String registrationNumber,
                                     String city) {
    }

    public record BadgeView(Badge.BadgeType type,
                            Badge.BadgeSource source,
                            String reason,
                            Instant grantedAt,
                            Instant expiresAt,
                            boolean active) {
    }

    public record RatingSummaryView(int reviewCount,
                                    BigDecimal averageScore,
                                    Map<Integer, Integer> distribution) {
    }

    public record PractitionerRatingView(String id,
                                         Long patientUserId,
                                         int score,
                                         String comment,
                                         Instant createdAt,
                                         boolean hidden) {
    }

    public record OpeningHoursView(String day, LocalTime opensAt, LocalTime closesAt,
                                   boolean closed, String note) {
    }

    public record LocationPhotoView(String storageKey, PracticeLocation.PhotoType type, String caption) {
    }

    public record PracticeLocationView(String id,
                                       Long practitionerUserId,
                                       String name,
                                       boolean mainLocation,
                                       String line1,
                                       String line2,
                                       String postalCode,
                                       String city,
                                       String country,
                                       Double latitude,
                                       Double longitude,
                                       String fullAddress,
                                       List<OpeningHoursView> openingHours,
                                       List<LocationPhotoView> photos,
                                       Map<String, String> socialLinks,
                                       String virtualTourUrl,
                                       String phone,
                                       String mobilePhone,
                                       String fax,
                                       String email,
                                       String website,
                                       boolean wheelchairAccessible,
                                       boolean parkingAvailable,
                                       String publicTransportInfo,
                                       boolean openNow) {
    }

    public record PractitionerDossierView(Long userId,
                                          PractitionerIdentityView identity,
                                          PracticeInformationView practiceInformation,
                                          ManagementView management,
                                          VisibilityProfileView visibility,
                                          List<NetworkContactView> networkContacts,
                                          RatingSummaryView ratingSummary,
                                          List<BadgeView> badges,
                                          List<PracticeLocationView> locations,
                                          boolean publishable,
                                          Instant updatedAt) {
    }

    /** Fiche annuaire (recherche publique de praticiens). */
    public record DirectoryEntryView(Long userId,
                                     String fullName,
                                     String mainSpecialty,
                                     List<String> languages,
                                     String city,
                                     BigDecimal averageRating,
                                     int reviewCount,
                                     List<Badge.BadgeType> badges,
                                     boolean teleconsultation,
                                     boolean wheelchairAccessible,
                                     boolean acceptsNewPatients) {
    }

    public record PagedResult<T>(List<T> items, long total, int page, int size) {
    }
}
