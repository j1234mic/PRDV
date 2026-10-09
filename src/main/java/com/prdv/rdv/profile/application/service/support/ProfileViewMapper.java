package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.medical.BodyMetrics;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import com.prdv.rdv.profile.domain.model.medical.VaccinationReminderCalculator;
import com.prdv.rdv.profile.domain.model.medical.VitalSigns;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import com.prdv.rdv.profile.domain.model.practitioner.Badge;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerRating;
import com.prdv.rdv.profile.domain.model.practitioner.RatingSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapper domaine -&gt; vues de lecture (pattern DTO + Mapper).
 *
 * <p>Aucune entite du domaine ni entite JPA ne franchit la frontiere web :
 * les controleurs ne manipulent que des records immuables, et les donnees
 * sensibles y sont absentes ou masquees.
 */
@Component
public class ProfileViewMapper {

    /** Fenetre d'anticipation des rappels de vaccination exposes dans le dossier. */
    public static final int REMINDER_LOOKAHEAD_DAYS = 60;

    private final Clock clock;

    public ProfileViewMapper(Clock clock) {
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // Identite patient
    // ------------------------------------------------------------------

    public ProfileViews.PatientIdentityView identityView(PatientIdentity identity) {
        PatientIdentity.CivilStatus civilStatus = identity.getCivilStatus();
        PatientIdentity.VitaleCard vitale = identity.getVitaleCard();
        PatientIdentity.DmpAccount dmp = identity.getDmpAccount();
        return new ProfileViews.PatientIdentityView(
                identity.getUserId(),
                civilStatus == null ? null : new ProfileViews.CivilStatusView(civilStatus.civility(),
                        civilStatus.firstName(), civilStatus.birthName(), civilStatus.lastName(),
                        civilStatus.preferredName(), civilStatus.birthDate(), civilStatus.birthPlace(),
                        civilStatus.birthCountry(), civilStatus.gender(), civilStatus.maritalStatus(),
                        civilStatus.nationality()),
                identity.getContacts().stream().map(this::contactView).toList(),
                identity.getAddresses().stream().map(this::addressView).toList(),
                emergencyContactView(identity.getEmergencyContact()),
                treatingPhysicianView(identity.getTreatingPhysician()),
                identity.getMaskedSocialSecurityNumber(),
                identity.getPhotoStorageKey() != null,
                identity.getIdentityDocumentStorageKey() != null,
                vitale == null ? null : new ProfileViews.VitaleCardView(vitale.cardVersion(),
                        vitale.issuedOn(), vitale.expiresOn(), vitale.readMode(),
                        vitale.scanStorageKey() != null, vitale.isExpired(LocalDate.now(clock)),
                        vitale.readAt()),
                insuranceView(identity.getPrimaryInsurance()),
                insuranceView(identity.getComplementaryInsurance()),
                otherInsuranceViews(identity),
                dmp == null ? null : new ProfileViews.DmpAccountView(dmp.dmpIdentifier(), dmp.linked(),
                        dmp.sharingEnabled(), dmp.linkedAt(), dmp.lastSyncAt()),
                identity.getCreatedAt(),
                identity.getUpdatedAt());
    }

    public ProfileViews.DmpAccountView dmpView(PatientIdentity.DmpAccount dmp) {
        return dmp == null ? null : new ProfileViews.DmpAccountView(dmp.dmpIdentifier(), dmp.linked(),
                dmp.sharingEnabled(), dmp.linkedAt(), dmp.lastSyncAt());
    }

    private ProfileViews.ContactPointView contactView(PatientIdentity.ContactPoint contact) {
        return new ProfileViews.ContactPointView(contact.type(), contact.value(), contact.verified(),
                contact.preferred());
    }

    private ProfileViews.PostalAddressView addressView(PatientIdentity.PostalAddress address) {
        return new ProfileViews.PostalAddressView(address.type(), address.line1(), address.line2(),
                address.postalCode(), address.city(), address.country(), address.latitude(),
                address.longitude(), address.isDefault());
    }

    private ProfileViews.EmergencyContactView emergencyContactView(
            PatientIdentity.EmergencyContact contact) {
        return contact == null ? null : new ProfileViews.EmergencyContactView(contact.firstName(),
                contact.lastName(), contact.relationship(), contact.phone(), contact.email());
    }

    private ProfileViews.TreatingPhysicianView treatingPhysicianView(
            PatientIdentity.TreatingPhysician physician) {
        return physician == null ? null : new ProfileViews.TreatingPhysicianView(physician.firstName(),
                physician.lastName(), physician.rppsNumber(), physician.phone(), physician.email(),
                physician.declaredToInsurance());
    }

    private List<ProfileViews.HealthInsuranceView> otherInsuranceViews(PatientIdentity identity) {
        if (identity.getOtherInsurances() == null) {
            return List.of();
        }
        return identity.getOtherInsurances().stream().map(this::insuranceView).toList();
    }

    private ProfileViews.HealthInsuranceView insuranceView(PatientIdentity.HealthInsurance insurance) {
        return insurance == null ? null : new ProfileViews.HealthInsuranceView(insurance.type(),
                insurance.organization(), insurance.memberNumber(), insurance.contractReference(),
                insurance.validUntil());
    }

    // ------------------------------------------------------------------
    // Dossier medical
    // ------------------------------------------------------------------

    public ProfileViews.MedicalRecordView medicalRecordView(MedicalRecord record) {
        LocalDate today = LocalDate.now(clock);
        return new ProfileViews.MedicalRecordView(
                record.getPatientUserId(),
                record.getBloodGroup() == null ? null : record.getBloodGroup().toString(),
                record.getHistory().stream().map(this::historyView).toList(),
                record.getFamilyHistory().stream().map(this::familyHistoryView).toList(),
                record.getAllergies().stream().map(this::allergyView).toList(),
                record.getVaccinations().stream().map(vaccination -> vaccinationView(vaccination, today)).toList(),
                record.getChronicConditions().stream().map(this::chronicConditionView).toList(),
                record.getTreatments().stream().map(this::treatmentView).toList(),
                record.getSurgeries().stream().map(this::surgeryView).toList(),
                record.getHospitalizations().stream().map(this::hospitalizationView).toList(),
                record.getDisabilities().stream().map(this::disabilityView).toList(),
                record.getLastVitalSigns() == null ? null
                        : vitalSignsView(record.getLastVitalSigns().vitalSigns()),
                VaccinationReminderCalculator.dueReminders(record, today, REMINDER_LOOKAHEAD_DAYS).stream()
                        .map(vaccination -> new ProfileViews.VaccinationReminderView(vaccination.id(),
                                vaccination.vaccine(), vaccination.nextReminderOn(),
                                VaccinationReminderCalculator.daysUntilReminder(vaccination, today),
                                VaccinationReminderCalculator.isOverdue(vaccination, today)))
                        .toList(),
                record.getUpdatedAt());
    }

    public List<ProfileViews.VaccinationReminderView> reminderViews(MedicalRecord record, int lookaheadDays) {
        LocalDate today = LocalDate.now(clock);
        return VaccinationReminderCalculator.dueReminders(record, today, lookaheadDays).stream()
                .map(vaccination -> new ProfileViews.VaccinationReminderView(vaccination.id(),
                        vaccination.vaccine(), vaccination.nextReminderOn(),
                        VaccinationReminderCalculator.daysUntilReminder(vaccination, today),
                        VaccinationReminderCalculator.isOverdue(vaccination, today)))
                .toList();
    }

    private ProfileViews.HistoryEntryView historyView(MedicalRecord.MedicalHistoryEntry entry) {
        return new ProfileViews.HistoryEntryView(entry.id(), entry.code(), entry.label(),
                entry.diagnosedOn(), entry.status(), entry.note(), entry.source());
    }

    private ProfileViews.FamilyHistoryView familyHistoryView(MedicalRecord.FamilyHistoryEntry entry) {
        return new ProfileViews.FamilyHistoryView(entry.id(), entry.relation(), entry.condition(),
                entry.relativeAgeAtDiagnosis(), entry.note());
    }

    private ProfileViews.AllergyView allergyView(MedicalRecord.Allergy allergy) {
        return new ProfileViews.AllergyView(allergy.id(), allergy.allergen(), allergy.type(),
                allergy.severity(), allergy.reaction(), allergy.declaredOn(), allergy.active());
    }

    private ProfileViews.VaccinationView vaccinationView(MedicalRecord.Vaccination vaccination,
                                                         LocalDate today) {
        return new ProfileViews.VaccinationView(vaccination.id(), vaccination.vaccine(),
                vaccination.dose(), vaccination.administeredOn(), vaccination.nextReminderOn(),
                vaccination.provider(), vaccination.mandatory(),
                VaccinationReminderCalculator.isOverdue(vaccination, today));
    }

    private ProfileViews.ChronicConditionView chronicConditionView(MedicalRecord.ChronicCondition condition) {
        return new ProfileViews.ChronicConditionView(condition.id(), condition.code(), condition.label(),
                condition.since(), condition.longTermCondition(), condition.note());
    }

    private ProfileViews.TreatmentView treatmentView(MedicalRecord.Treatment treatment) {
        return new ProfileViews.TreatmentView(treatment.id(), treatment.drug(), treatment.dosage(),
                treatment.frequency(), treatment.prescriber(), treatment.startedOn(), treatment.endedOn(),
                treatment.isOngoing(), treatment.note());
    }

    private ProfileViews.SurgeryView surgeryView(MedicalRecord.Surgery surgery) {
        return new ProfileViews.SurgeryView(surgery.id(), surgery.procedure(), surgery.performedOn(),
                surgery.establishment(), surgery.surgeon(), surgery.note());
    }

    private ProfileViews.HospitalizationView hospitalizationView(MedicalRecord.Hospitalization stay) {
        return new ProfileViews.HospitalizationView(stay.id(), stay.reason(), stay.establishment(),
                stay.admittedOn(), stay.dischargedOn(), stay.note());
    }

    private ProfileViews.DisabilityView disabilityView(MedicalRecord.Disability disability) {
        return new ProfileViews.DisabilityView(disability.id(), disability.type(),
                disability.disabilityRatePercent(), disability.aids(), disability.note());
    }

    public ProfileViews.BodyMetricsView bodyMetricsView(BodyMetrics metrics) {
        return metrics == null ? null : new ProfileViews.BodyMetricsView(metrics.heightCm(),
                metrics.weightKg(), metrics.bmi(), metrics.category().name());
    }

    public ProfileViews.VitalSignsView vitalSignsView(VitalSigns signs) {
        return signs == null ? null : new ProfileViews.VitalSignsView(signs.systolicMmHg(),
                signs.diastolicMmHg(), signs.heartRateBpm(), signs.temperatureCelsius(),
                signs.oxygenSaturationPercent(), signs.respiratoryRatePerMin(),
                bodyMetricsView(signs.bodyMetrics()), signs.measuredAt(),
                signs.isHypertensive(), signs.hasFever(), signs.isHypoxemic());
    }

    // ------------------------------------------------------------------
    // Documents medicaux
    // ------------------------------------------------------------------

    public ProfileViews.MedicalDocumentView documentView(MedicalDocument document) {
        List<MedicalDocument.DocumentVersion> versions = document.getVersions();
        MedicalDocument.DocumentVersion current = versions.isEmpty() ? null : versions.get(versions.size() - 1);
        MedicalDocument.ClassificationResult classification = document.getClassification();
        MedicalDocument.OcrResult ocr = document.getOcrResult();
        return new ProfileViews.MedicalDocumentView(
                document.getId(),
                document.getOwnerUserId(),
                document.getTitle(),
                document.getCategory(),
                document.getStatus(),
                classification == null ? null : classification.source(),
                classification == null ? null : classification.confidence(),
                ocr == null ? null : new ProfileViews.OcrResultView(ocr.status(), ocr.extractedFields(),
                        ocr.confidence(), ocr.engine(), ocr.processedAt()),
                document.getDicomModality(),
                document.getDicomStudyDescription(),
                document.currentVersionNumber(),
                current == null ? null : current.originalFilename(),
                current == null ? null : current.contentType(),
                current == null ? 0 : current.sizeBytes(),
                document.getDmpReference(),
                document.getDmpSharedAt(),
                versions.stream().map(this::versionView).toList(),
                document.getShares().stream()
                        .sorted(Comparator.comparing(MedicalDocument.DocumentShare::grantedAt))
                        .map(share -> shareView(share, clock.instant()))
                        .toList(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private ProfileViews.DocumentVersionView versionView(MedicalDocument.DocumentVersion version) {
        return new ProfileViews.DocumentVersionView(version.version(), version.originalFilename(),
                version.contentType(), version.sizeBytes(), version.sha256(), version.changeNote(),
                version.uploadedBy(), version.uploadedAt());
    }

    public ProfileViews.DocumentShareView shareView(MedicalDocument.DocumentShare share, Instant now) {
        return new ProfileViews.DocumentShareView(share.id(), share.granteeUserId(), share.permission(),
                share.reason(), share.grantedAt(), share.expiresAt(), share.revokedAt(),
                share.isActive(now));
    }

    // ------------------------------------------------------------------
    // Sante connectee
    // ------------------------------------------------------------------

    public ProfileViews.ConnectedDeviceView deviceView(ConnectedDevice device) {
        return new ProfileViews.ConnectedDeviceView(device.getId(), device.getType(), device.getProvider(),
                device.getLabel(), device.getModel(), device.getStatus(), device.getLastSyncError(),
                device.getConnectedAt(), device.getLastSyncAt(), device.getSyncedMetricsCount());
    }

    public ProfileViews.HealthMetricView metricView(HealthMetric metric) {
        return new ProfileViews.HealthMetricView(metric.id(), metric.type(), metric.value(),
                metric.type().unit(), metric.context(), metric.recordedAt(), metric.deviceId(),
                metric.sourceLabel());
    }

    public ProfileViews.HealthAlertView alertView(HealthAlert alert) {
        return new ProfileViews.HealthAlertView(alert.getId(), alert.getMetricType(),
                alert.getObservedValue(), alert.getThreshold(), alert.getSeverity(), alert.getMessage(),
                alert.getTriggeredAt(), alert.isAcknowledged());
    }

    // ------------------------------------------------------------------
    // Preferences
    // ------------------------------------------------------------------

    public ProfileViews.PrivacyPreferencesView preferencesView(PrivacyPreferences preferences) {
        PrivacyPreferences.AccessibilitySettings accessibility = preferences.getAccessibility();
        PrivacyPreferences.CommunicationSettings communication = preferences.getCommunication();
        List<ProfileViews.ActiveConsentView> consents = new ArrayList<>();
        for (PrivacyPreferences.ConsentPurpose purpose : PrivacyPreferences.ConsentPurpose.values()) {
            preferences.consentFor(purpose).ifPresent(consent -> consents.add(
                    new ProfileViews.ActiveConsentView(purpose.name(), consent.granted(),
                            consent.policyVersion(), consent.recordedAt(), consent.withdrawnAt())));
        }
        List<ProfileViews.VisibilityRuleView> rules = preferences.getVisibilityRules().stream()
                .sorted(Comparator.comparing(rule -> rule.category().name()))
                .map(rule -> new ProfileViews.VisibilityRuleView(rule.category().name(),
                        rule.level().name(), rule.granteeUserIds().stream().sorted().toList()))
                .toList();
        return new ProfileViews.PrivacyPreferencesView(
                preferences.getUserId(),
                List.copyOf(preferences.getPreferredLanguages()),
                preferences.getPrimaryLanguage(),
                accessibility == null ? null : new ProfileViews.AccessibilityView(
                        accessibility.needs().stream().map(Enum::name).sorted().toList(),
                        accessibility.screenReaderOptimized(), accessibility.largePrint(),
                        accessibility.signLanguage(), accessibility.subtitlesRequired()),
                communication == null ? null : new ProfileViews.CommunicationView(
                        communication.preferredChannels().stream().map(Enum::name).sorted().toList(),
                        communication.quietHoursEnabled(), communication.quietHoursStart(),
                        communication.quietHoursEnd()),
                consents,
                rules,
                preferences.isDmpSharingEnabled(),
                preferences.getErasureRequestedAt(),
                preferences.getUpdatedAt());
    }

    // ------------------------------------------------------------------
    // Praticien
    // ------------------------------------------------------------------

    public ProfileViews.PractitionerDossierView dossierView(PractitionerDossier dossier,
                                                            RatingSummary ratingSummary,
                                                            List<Badge> badges,
                                                            List<PracticeLocation> locations) {
        LocalDateTime now = LocalDateTime.now(clock);
        RatingSummary summary = ratingSummary == null ? RatingSummary.empty() : ratingSummary;
        return new ProfileViews.PractitionerDossierView(
                dossier.getUserId(),
                identityView(dossier.getIdentity()),
                practiceInformationView(dossier.getPracticeInformation()),
                managementView(dossier.getManagement()),
                visibilityView(dossier.getVisibility()),
                dossier.getNetworkContacts().stream().map(this::networkContactView).toList(),
                ratingSummaryView(summary),
                badges.stream().map(badge -> badgeView(badge, clock.instant())).toList(),
                locations.stream().map(location -> locationView(location, now)).toList(),
                dossier.isPublishable(),
                dossier.getUpdatedAt());
    }

    public ProfileViews.PractitionerIdentityView identityView(PractitionerDossier.ProfessionalIdentity identity) {
        if (identity == null) {
            return null;
        }
        return new ProfileViews.PractitionerIdentityView(identity.title(), identity.firstName(),
                identity.lastName(), identity.rppsNumber(), identity.adeliNumber(),
                identity.registrationOrder(), identity.mainSpecialty(), identity.secondarySpecialties(),
                identity.subSpecialties(), identity.skills(),
                identity.diplomas().stream().map(diploma -> new ProfileViews.DiplomaView(diploma.label(),
                        diploma.institution(), diploma.year(), diploma.specialty())).toList(),
                identity.experienceYears(), identity.languages(),
                identity.photoStorageKey() != null, identity.presentationVideoKey() != null,
                identity.shortBio());
    }

    public ProfileViews.PracticeInformationView practiceInformationView(
            PractitionerDossier.PracticeInformation information) {
        if (information == null) {
            return null;
        }
        PractitionerDossier.AgeRange ages = information.acceptedAges();
        return new ProfileViews.PracticeInformationView(information.sector(),
                information.tariffs().stream().map(tariff -> new ProfileViews.TariffView(tariff.actType(),
                        tariff.amount(), tariff.currency(), tariff.coveredByInsurance(), tariff.note()))
                        .toList(),
                information.optam(), information.optamCo(), information.optamSignedOn(),
                information.paymentMethods().stream().map(Enum::name).sorted().toList(),
                information.thirdPartyPayment(), information.thirdPartyPaymentConditions(),
                information.actsPerformed(), information.availableEquipment(),
                information.treatedPathologies(),
                ages == null ? null : new ProfileViews.AgeRangeView(ages.minAge(), ages.maxAge(),
                        ages.acceptsChildren(), ages.acceptsAdults(), ages.acceptsSeniors()),
                information.teleconsultation(), information.teleconsultationPlatform());
    }

    public ProfileViews.ManagementView managementView(PractitionerDossier.ProfessionalManagement management) {
        if (management == null) {
            return null;
        }
        return new ProfileViews.ManagementView(management.siret(), management.maskedIban(),
                management.professionalInsurer(), management.insurancePolicyNumber(),
                management.insuranceExpiresOn(), management.urssafNumber(),
                management.accountingAssociation(), management.accountingAssociationNumber(),
                management.ordinalCotisationYear(), management.ordinalCotisationAmount(),
                management.qualityCertifications(),
                management.accreditations().stream().map(accreditation ->
                        new ProfileViews.AccreditationView(accreditation.organism(),
                                accreditation.reference(), accreditation.validUntil())).toList());
    }

    public ProfileViews.VisibilityProfileView visibilityView(PractitionerDossier.VisibilityProfile profile) {
        if (profile == null) {
            return null;
        }
        return new ProfileViews.VisibilityProfileView(profile.longDescription(), profile.expertiseDomains(),
                profile.publications(), profile.distinctions(), profile.conferenceTalks(),
                profile.mediaReferences());
    }

    private ProfileViews.NetworkContactView networkContactView(PractitionerDossier.NetworkContact contact) {
        return new ProfileViews.NetworkContactView(contact.id(), contact.role(), contact.name(),
                contact.specialty(), contact.registrationNumber(), contact.city());
    }

    public ProfileViews.BadgeView badgeView(Badge badge, Instant now) {
        return new ProfileViews.BadgeView(badge.type(), badge.source(), badge.reason(), badge.grantedAt(),
                badge.expiresAt(), badge.isActive(now));
    }

    public ProfileViews.RatingSummaryView ratingSummaryView(RatingSummary summary) {
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int star = RatingSummary.MAX_STARS; star >= 1; star--) {
            distribution.put(star, summary.distribution().getOrDefault(star, 0));
        }
        return new ProfileViews.RatingSummaryView(summary.reviewCount(), summary.averageScore(),
                distribution);
    }

    public ProfileViews.PractitionerRatingView ratingView(PractitionerRating rating) {
        return new ProfileViews.PractitionerRatingView(rating.getId(), rating.getPatientUserId(),
                rating.getScore(), rating.getComment(), rating.getCreatedAt(), rating.isHidden());
    }

    public ProfileViews.PracticeLocationView locationView(PracticeLocation location, LocalDateTime now) {
        return new ProfileViews.PracticeLocationView(location.getId(), location.getPractitionerUserId(),
                location.getName(), location.isMainLocation(), location.getLine1(), location.getLine2(),
                location.getPostalCode(), location.getCity(), location.getCountry(),
                location.getLatitude(), location.getLongitude(), location.fullAddress(),
                location.getOpeningHours().stream()
                        .sorted(Comparator.comparingInt(slot -> slot.day().getValue()))
                        .map(slot -> new ProfileViews.OpeningHoursView(slot.day().name(), slot.opensAt(),
                                slot.closesAt(), slot.closed(), slot.note()))
                        .toList(),
                location.getPhotos().stream()
                        .map(photo -> new ProfileViews.LocationPhotoView(photo.photoId(), photo.storageKey(),
                                photo.type(), photo.caption()))
                        .toList(),
                Map.copyOf(location.getSocialLinks()),
                location.getVirtualTourUrl(), location.getPhone(), location.getMobilePhone(),
                location.getFax(), location.getEmail(), location.getWebsite(),
                location.isWheelchairAccessible(), location.isParkingAvailable(),
                location.getPublicTransportInfo(), location.isOpenAt(now));
    }

    public ProfileViews.DirectoryEntryView directoryEntryView(PractitionerDossier dossier,
                                                              RatingSummary summary,
                                                              List<Badge.BadgeType> badges,
                                                              String city,
                                                              boolean wheelchairAccessible) {
        PractitionerDossier.ProfessionalIdentity identity = dossier.getIdentity();
        PractitionerDossier.PracticeInformation information = dossier.getPracticeInformation();
        return new ProfileViews.DirectoryEntryView(dossier.getUserId(),
                identity == null ? null : identity.fullName(),
                identity == null ? null : identity.mainSpecialty(),
                identity == null ? List.of() : identity.languages(),
                city,
                summary == null ? BigDecimal.ZERO : summary.averageScore(),
                summary == null ? 0 : summary.reviewCount(),
                badges,
                information != null && information.teleconsultation(),
                wheelchairAccessible,
                true);
    }

    /** Vue synthetique des badges actifs, pour l'annuaire. */
    public List<Badge.BadgeType> activeBadgeTypes(List<Badge> badges) {
        Instant now = clock.instant();
        return badges.stream().filter(badge -> badge.isActive(now)).map(Badge::type).distinct().toList();
    }

    /** Ensemble trié et immuable (utilise par les vues exposant des identifiants). */
    public List<Long> sortedIds(Set<Long> ids) {
        return ids == null ? List.of() : ids.stream().sorted().toList();
    }
}
