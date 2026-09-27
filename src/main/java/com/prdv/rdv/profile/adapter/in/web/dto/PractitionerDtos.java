package com.prdv.rdv.profile.adapter.in.web.dto;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** DTO d'entree du profil professionnel, des cabinets et de l'annuaire. */
public final class PractitionerDtos {

    private PractitionerDtos() {
    }

    // ------------------------------------------------------------------
    // Identite professionnelle
    // ------------------------------------------------------------------

    public record DiplomaRequest(@NotBlank @Size(max = 200) String label,
                                 @Size(max = 200) String institution,
                                 @Min(1900) @Max(2100) Integer year,
                                 @Size(max = 150) String specialty) {

        public PractitionerDossier.Diploma toValue() {
            return new PractitionerDossier.Diploma(label, institution, year, specialty);
        }
    }

    public record IdentityRequest(PractitionerDossier.Title title,
                                  @NotBlank @Size(max = 100) String firstName,
                                  @NotBlank @Size(max = 100) String lastName,
                                  @Size(max = 30) String rppsNumber,
                                  @Size(max = 30) String adeliNumber,
                                  PractitionerDossier.RegistrationOrder registrationOrder,
                                  @Size(max = 150) String mainSpecialty,
                                  List<String> secondarySpecialties,
                                  List<String> subSpecialties,
                                  List<String> skills,
                                  List<@Valid DiplomaRequest> diplomas,
                                  @Min(0) @Max(80) Integer experienceYears,
                                  List<String> languages,
                                  @Size(max = 1000) String shortBio) {

        /**
         * La photo et la video sont televersees separement : les cles de stockage
         * ne sont jamais fournies par le client. Le service conserve les cles
         * existantes lorsque le formulaire n'en apporte pas.
         */
        public PractitionerProfileCommands.UpdateIdentity toCommand() {
            return new PractitionerProfileCommands.UpdateIdentity(toValue(null, null));
        }

        public PractitionerDossier.ProfessionalIdentity toValue(String photoStorageKey,
                                                                String presentationVideoKey) {
            return new PractitionerDossier.ProfessionalIdentity(title, firstName, lastName, rppsNumber,
                    adeliNumber, registrationOrder, mainSpecialty, secondarySpecialties, subSpecialties,
                    skills, diplomas == null ? List.of() : diplomas.stream().map(DiplomaRequest::toValue).toList(),
                    experienceYears, languages, photoStorageKey, presentationVideoKey, shortBio);
        }
    }

    // ------------------------------------------------------------------
    // Informations medicales / tarifs
    // ------------------------------------------------------------------

    public record TariffRequest(@NotBlank @Size(max = 120) String actType,
                                @NotNull @Min(0) BigDecimal amount,
                                @Size(max = 3) String currency,
                                boolean coveredByInsurance,
                                @Size(max = 255) String note) {

        public PractitionerDossier.Tariff toValue() {
            return new PractitionerDossier.Tariff(actType, amount,
                    currency == null || currency.isBlank() ? "EUR" : currency, coveredByInsurance, note);
        }
    }

    public record PracticeInformationRequest(@NotNull PractitionerDossier.ConventionSector sector,
                                             List<@Valid TariffRequest> tariffs,
                                             boolean optam,
                                             boolean optamCo,
                                             LocalDate optamSignedOn,
                                             Set<PractitionerDossier.PaymentMethod> paymentMethods,
                                             boolean thirdPartyPayment,
                                             @Size(max = 500) String thirdPartyPaymentConditions,
                                             List<String> actsPerformed,
                                             List<String> availableEquipment,
                                             List<String> treatedPathologies,
                                             @Min(0) @Max(130) Integer acceptedAgeMin,
                                             @Min(0) @Max(130) Integer acceptedAgeMax,
                                             boolean teleconsultation,
                                             @Size(max = 150) String teleconsultationPlatform) {

        public PractitionerProfileCommands.UpdatePracticeInformation toCommand() {
            return new PractitionerProfileCommands.UpdatePracticeInformation(
                    new PractitionerDossier.PracticeInformation(sector,
                            tariffs == null ? List.of() : tariffs.stream().map(TariffRequest::toValue).toList(),
                            optam, optamCo, optamSignedOn, paymentMethods, thirdPartyPayment,
                            thirdPartyPaymentConditions, actsPerformed, availableEquipment,
                            treatedPathologies, new PractitionerDossier.AgeRange(acceptedAgeMin,
                            acceptedAgeMax), teleconsultation, teleconsultationPlatform));
        }
    }

    // ------------------------------------------------------------------
    // Gestion professionnelle
    // ------------------------------------------------------------------

    public record AccreditationRequest(@NotBlank @Size(max = 200) String organism,
                                       @Size(max = 100) String reference,
                                       LocalDate validUntil) {

        public PractitionerDossier.Accreditation toValue() {
            return new PractitionerDossier.Accreditation(organism, reference, validUntil);
        }
    }

    public record ManagementRequest(@Size(max = 14) String siret,
                                    String iban,
                                    @Size(max = 200) String professionalInsurer,
                                    @Size(max = 100) String insurancePolicyNumber,
                                    LocalDate insuranceExpiresOn,
                                    @Size(max = 100) String urssafNumber,
                                    @Size(max = 10) String accountingAssociation,
                                    @Size(max = 100) String accountingAssociationNumber,
                                    @Min(2000) @Max(2100) Integer ordinalCotisationYear,
                                    @Min(0) BigDecimal ordinalCotisationAmount,
                                    List<String> qualityCertifications,
                                    List<@Valid AccreditationRequest> accreditations) {

        /**
         * Le token et le masque du RIB sont calcules par le service : le client
         * ne fournit que l'IBAN brut, qui n'est jamais persiste.
         */
        public PractitionerProfileCommands.UpdateManagement toCommand() {
            return new PractitionerProfileCommands.UpdateManagement(
                    new PractitionerDossier.ProfessionalManagement(siret, null, null,
                            professionalInsurer, insurancePolicyNumber, insuranceExpiresOn, urssafNumber,
                            accountingAssociation, accountingAssociationNumber, ordinalCotisationYear,
                            ordinalCotisationAmount, qualityCertifications,
                            accreditations == null ? List.of()
                                    : accreditations.stream().map(AccreditationRequest::toValue).toList()),
                    iban);
        }
    }

    // ------------------------------------------------------------------
    // Visibilite et reseau
    // ------------------------------------------------------------------

    public record VisibilityRequest(@Size(max = 5000) String longDescription,
                                    List<String> expertiseDomains,
                                    List<String> publications,
                                    List<String> distinctions,
                                    List<String> conferenceTalks,
                                    List<String> mediaReferences) {

        public PractitionerProfileCommands.UpdateVisibility toCommand() {
            return new PractitionerProfileCommands.UpdateVisibility(
                    new PractitionerDossier.VisibilityProfile(longDescription, expertiseDomains,
                            publications, distinctions, conferenceTalks, mediaReferences));
        }
    }

    public record NetworkContactRequest(@NotNull PractitionerDossier.NetworkRole role,
                                        @NotBlank @Size(max = 200) String name,
                                        @Size(max = 150) String specialty,
                                        @Size(max = 50) String registrationNumber,
                                        @Size(max = 120) String city) {

        public PractitionerProfileCommands.AddNetworkContact toCommand() {
            return new PractitionerProfileCommands.AddNetworkContact(role, name, specialty,
                    registrationNumber, city);
        }
    }

    // ------------------------------------------------------------------
    // Cabinets
    // ------------------------------------------------------------------

    public record OpeningHoursRequest(@NotNull DayOfWeek day,
                                      LocalTime opensAt,
                                      LocalTime closesAt,
                                      boolean closed,
                                      @Size(max = 255) String note) {

        public PractitionerProfileCommands.OpeningHoursEntry toEntry() {
            return new PractitionerProfileCommands.OpeningHoursEntry(day, opensAt, closesAt, closed, note);
        }
    }

    public record LocationRequest(@NotBlank @Size(max = 200) String name,
                                  @NotBlank @Size(max = 255) String line1,
                                  @Size(max = 255) String line2,
                                  @Size(max = 10) String postalCode,
                                  @NotBlank @Size(max = 120) String city,
                                  @Size(max = 100) String country,
                                  Double latitude,
                                  Double longitude,
                                  List<@Valid OpeningHoursRequest> openingHours,
                                  @Size(max = 50) String phone,
                                  @Size(max = 50) String mobilePhone,
                                  @Size(max = 50) String fax,
                                  @Size(max = 255) String email,
                                  @Size(max = 255) String website,
                                  @Size(max = 500) String virtualTourUrl,
                                  Map<String, String> socialLinks,
                                  boolean wheelchairAccessible,
                                  boolean parkingAvailable,
                                  @Size(max = 255) String publicTransportInfo) {

        public PractitionerProfileCommands.CreateLocation toCreateCommand(boolean mainLocation) {
            return new PractitionerProfileCommands.CreateLocation(name, line1, line2, postalCode, city,
                    country, mainLocation, latitude, longitude, hours(), phone, mobilePhone, fax, email,
                    website, virtualTourUrl, socialLinks, wheelchairAccessible, parkingAvailable,
                    publicTransportInfo);
        }

        public PractitionerProfileCommands.UpdateLocation toUpdateCommand() {
            return new PractitionerProfileCommands.UpdateLocation(name, line1, line2, postalCode, city,
                    country, latitude, longitude, hours(), phone, mobilePhone, fax, email, website,
                    virtualTourUrl, socialLinks, wheelchairAccessible, parkingAvailable,
                    publicTransportInfo);
        }

        private List<PractitionerProfileCommands.OpeningHoursEntry> hours() {
            return openingHours == null ? List.of()
                    : openingHours.stream().map(OpeningHoursRequest::toEntry).toList();
        }
    }

    public record LocationPhotoRequest(@NotNull PracticeLocation.PhotoType type,
                                       @Size(max = 255) String caption) {

        public PractitionerProfileCommands.AddLocationPhoto toCommand(String locationId,
                                                                      MultipartFile file) {
            return new PractitionerProfileCommands.AddLocationPhoto(locationId, type,
                    PatientIdentityDtos.filename(file), PatientIdentityDtos.contentType(file),
                    PatientIdentityDtos.content(file), caption);
        }
    }

    /** Photo de profil ou video de presentation : multipart uniquement. */
    public static PractitionerProfileCommands.StoreMedia toStoreMedia(MultipartFile file, boolean video) {
        return new PractitionerProfileCommands.StoreMedia(PatientIdentityDtos.filename(file),
                PatientIdentityDtos.contentType(file), PatientIdentityDtos.content(file), video);
    }

    // ------------------------------------------------------------------
    // Avis patients
    // ------------------------------------------------------------------

    public record RatingRequest(@Min(1) @Max(5) int score, @Size(max = 1000) String comment) {

        public PractitionerProfileCommands.SubmitRating toCommand() {
            return new PractitionerProfileCommands.SubmitRating(score, comment);
        }
    }
}
