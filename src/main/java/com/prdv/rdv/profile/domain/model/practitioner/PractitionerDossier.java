package com.prdv.rdv.profile.domain.model.practitioner;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Agregat racine « Dossier professionnel du praticien » (module 2.2).
 *
 * <p>Regroupe l'identite professionnelle (titre, RPPS/ADELI, ordre,
 * specialites, competences, diplomes, langues, photo, video), les
 * informations medicales (secteur de convention, tarifs, OPTAM, modes de
 * paiement, tiers-payant, actes, equipements, pathologies, ages acceptes,
 * teleconsultation), la gestion professionnelle (SIRET, RIB tokenise, RC pro,
 * URSSAF, AGA/CGA, cotisation ordinale, certifications, accreditations), le
 * reseau professionnel et le contenu de visibilite.
 *
 * <p>Les lieux d'exercice, les avis et les badges sont des agregats distincts
 * ({@link PracticeLocation}, {@link PractitionerRating}, {@link Badge}) :
 * leur cycle de vie et leur volumetrie differents justifient des frontieres
 * separatees (Single Responsibility, agregats petits).
 */
@Getter
@Setter
public class PractitionerDossier {

    // ------------------------------------------------------------------
    // Enumerations
    // ------------------------------------------------------------------

    public enum Title { DOCTOR, PROFESSOR, MISTER, MISS, MRS }

    public enum RegistrationOrder {
        PHYSICIANS, DENTAL_SURGEONS, MIDWIVES, PHARMACISTS, NURSES,
        PHYSIOTHERAPISTS, PODIATRISTS, SPEECH_THERAPISTS, ORTHOPTISTS, OTHER
    }

    public enum ConventionSector { SECTOR_1, SECTOR_2, SECTOR_3 }

    public enum PaymentMethod { CASH, CHECK, CREDIT_CARD, BANK_TRANSFER, THIRD_PARTY_PAYMENT }

    public enum NetworkRole { CORRESPONDENT, SPECIALIST, LABORATORY, PHARMACY, IMAGING_CENTER, HOSPITAL }

    // ------------------------------------------------------------------
    // Value Objects
    // ------------------------------------------------------------------

    public record Diploma(String label, String institution, Integer year, String specialty) {

        public Diploma {
            if (label == null || label.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "L'intitule du diplome est obligatoire");
            }
            if (year != null && (year < 1900 || year > LocalDate.now().getYear() + 1)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Annee de diplome incoherente : " + year);
            }
            label = label.trim();
        }
    }

    public record Accreditation(String organism, String reference, LocalDate validUntil) {
    }

    /** Tarif par type d'acte : la base de la transparence tarifaire. */
    public record Tariff(String actType,
                         BigDecimal amount,
                         String currency,
                         boolean coveredByInsurance,
                         String note) {

        public Tariff {
            if (actType == null || actType.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le type d'acte est obligatoire");
            }
            if (amount == null || amount.signum() < 0) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le tarif doit etre un montant positif");
            }
            actType = actType.trim();
            currency = currency == null || currency.isBlank() ? "EUR" : currency.toUpperCase(java.util.Locale.ROOT);
        }
    }

    /** Tranche d'age acceptee (enfants, adultes, seniors). */
    public record AgeRange(Integer minAge, Integer maxAge) {

        public AgeRange {
            if (minAge != null && maxAge != null && minAge > maxAge) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "L'age minimum ne peut pas depasser l'age maximum");
            }
            if (minAge != null && (minAge < 0 || minAge > 120)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Age minimum incoherent : " + minAge);
            }
            if (maxAge != null && (maxAge < 0 || maxAge > 120)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Age maximum incoherent : " + maxAge);
            }
        }

        public boolean accepts(int age) {
            return (minAge == null || age >= minAge) && (maxAge == null || age <= maxAge);
        }

        public boolean acceptsChildren() {
            return accepts(6);
        }

        public boolean acceptsAdults() {
            return accepts(35);
        }

        public boolean acceptsSeniors() {
            return accepts(75);
        }
    }

    /** Identite professionnelle complete. */
    public record ProfessionalIdentity(Title title,
                                       String firstName,
                                       String lastName,
                                       String rppsNumber,
                                       String adeliNumber,
                                       RegistrationOrder registrationOrder,
                                       String mainSpecialty,
                                       List<String> secondarySpecialties,
                                       List<String> subSpecialties,
                                       List<String> skills,
                                       List<Diploma> diplomas,
                                       Integer experienceYears,
                                       List<String> languages,
                                       String photoStorageKey,
                                       String presentationVideoKey,
                                       String shortBio) {

        public ProfessionalIdentity {
            if (lastName == null || lastName.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le nom du praticien est obligatoire");
            }
            if ((rppsNumber == null || rppsNumber.isBlank())
                    && (adeliNumber == null || adeliNumber.isBlank())) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Un numero RPPS ou ADELI est obligatoire");
            }
            if (experienceYears != null && (experienceYears < 0 || experienceYears > 70)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Nombre d'annees d'experience incoherent : " + experienceYears);
            }
            lastName = lastName.trim();
            secondarySpecialties = copyOf(secondarySpecialties);
            subSpecialties = copyOf(subSpecialties);
            skills = copyOf(skills);
            languages = copyOf(languages);
            diplomas = diplomas == null ? List.of() : List.copyOf(diplomas);
        }

        public String fullName() {
            return (firstName == null || firstName.isBlank() ? "" : firstName + " ") + lastName;
        }

        /** Copie avec la photo professionnelle renseignee (les Value Objects sont immuables). */
        public ProfessionalIdentity withPhoto(String newPhotoStorageKey) {
            return new ProfessionalIdentity(title, firstName, lastName, rppsNumber, adeliNumber,
                    registrationOrder, mainSpecialty, secondarySpecialties, subSpecialties, skills,
                    diplomas, experienceYears, languages, newPhotoStorageKey, presentationVideoKey,
                    shortBio);
        }

        /** Copie avec la video de presentation renseignee. */
        public ProfessionalIdentity withPresentationVideo(String newVideoStorageKey) {
            return new ProfessionalIdentity(title, firstName, lastName, rppsNumber, adeliNumber,
                    registrationOrder, mainSpecialty, secondarySpecialties, subSpecialties, skills,
                    diplomas, experienceYears, languages, photoStorageKey, newVideoStorageKey,
                    shortBio);
        }
    }

    /** Informations medicales affichees aux patients (tarifs, actes, acces...). */
    public record PracticeInformation(ConventionSector sector,
                                      List<Tariff> tariffs,
                                      boolean optam,
                                      boolean optamCo,
                                      LocalDate optamSignedOn,
                                      Set<PaymentMethod> paymentMethods,
                                      boolean thirdPartyPayment,
                                      String thirdPartyPaymentConditions,
                                      List<String> actsPerformed,
                                      List<String> availableEquipment,
                                      List<String> treatedPathologies,
                                      AgeRange acceptedAges,
                                      boolean teleconsultation,
                                      String teleconsultationPlatform) {

        public PracticeInformation {
            tariffs = tariffs == null ? List.of() : List.copyOf(tariffs);
            paymentMethods = paymentMethods == null ? Set.of() : Set.copyOf(paymentMethods);
            actsPerformed = copyOf(actsPerformed);
            availableEquipment = copyOf(availableEquipment);
            treatedPathologies = copyOf(treatedPathologies);
            if (optamCo && !optam) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "L'adhesion OPTAM-CO suppose une adhesion OPTAM");
            }
        }

        public static PracticeInformation defaults() {
            return new PracticeInformation(ConventionSector.SECTOR_1, List.of(), false, false, null,
                    Set.of(PaymentMethod.CREDIT_CARD, PaymentMethod.CHECK, PaymentMethod.CASH),
                    false, null, List.of(), List.of(), List.of(), new AgeRange(0, 120), false, null);
        }
    }

    /** Elements de gestion professionnelle (administratif, assurance, ordres). */
    public record ProfessionalManagement(String siret,
                                         String ribToken,
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
                                         List<Accreditation> accreditations) {

        public ProfessionalManagement {
            qualityCertifications = copyOf(qualityCertifications);
            accreditations = accreditations == null ? List.of() : List.copyOf(accreditations);
        }

        public boolean isInsuranceValidOn(LocalDate date) {
            return professionalInsurer != null && !professionalInsurer.isBlank()
                    && insuranceExpiresOn != null && !insuranceExpiresOn.isBefore(date);
        }
    }

    /** Contenu de visibilite et de marketing (description, publications, medias...). */
    public record VisibilityProfile(String longDescription,
                                    List<String> expertiseDomains,
                                    List<String> publications,
                                    List<String> distinctions,
                                    List<String> conferenceTalks,
                                    List<String> mediaReferences) {

        public VisibilityProfile {
            expertiseDomains = copyOf(expertiseDomains);
            publications = copyOf(publications);
            distinctions = copyOf(distinctions);
            conferenceTalks = copyOf(conferenceTalks);
            mediaReferences = copyOf(mediaReferences);
        }

        public static VisibilityProfile empty() {
            return new VisibilityProfile(null, List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    /** Contact du reseau professionnel (correspondant, laboratoire, pharmacie...). */
    public record NetworkContact(String id,
                                 NetworkRole role,
                                 String name,
                                 String specialty,
                                 String registrationNumber,
                                 String city) {

        public NetworkContact {
            if (role == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le role du contact de reseau est obligatoire");
            }
            if (name == null || name.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le nom du contact de reseau est obligatoire");
            }
            name = name.trim();
        }
    }

    // ------------------------------------------------------------------
    // Etat de l'agregat
    // ------------------------------------------------------------------

    private Long id;
    private Long userId;

    private ProfessionalIdentity identity;
    private PracticeInformation practiceInformation = PracticeInformation.defaults();
    private ProfessionalManagement management;
    private VisibilityProfile visibility = VisibilityProfile.empty();

    private List<NetworkContact> networkContacts = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Fabrique
    // ------------------------------------------------------------------

    public static PractitionerDossier create(Long userId, ProfessionalIdentity identity, Clock clock) {
        if (userId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le dossier professionnel doit etre rattache a un utilisateur");
        }
        if (identity == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "L'identite professionnelle est obligatoire");
        }
        PractitionerDossier dossier = new PractitionerDossier();
        dossier.userId = userId;
        dossier.identity = identity;
        dossier.createdAt = clock.instant();
        dossier.updatedAt = dossier.createdAt;
        return dossier;
    }

    // ------------------------------------------------------------------
    // Comportements
    // ------------------------------------------------------------------

    public void updateIdentity(ProfessionalIdentity newIdentity, Clock clock) {
        if (newIdentity == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "L'identite professionnelle est obligatoire");
        }
        this.identity = newIdentity;
        touch(clock);
    }

    public void updatePracticeInformation(PracticeInformation information, Clock clock) {
        this.practiceInformation = information == null ? PracticeInformation.defaults() : information;
        touch(clock);
    }

    public void updateManagement(ProfessionalManagement newManagement, Clock clock) {
        this.management = newManagement;
        touch(clock);
    }

    public void updateVisibility(VisibilityProfile newVisibility, Clock clock) {
        this.visibility = newVisibility == null ? VisibilityProfile.empty() : newVisibility;
        touch(clock);
    }

    public NetworkContact addNetworkContact(NetworkRole role, String name, String specialty,
                                            String registrationNumber, String city, Clock clock) {
        NetworkContact contact = new NetworkContact(java.util.UUID.randomUUID().toString(), role, name,
                specialty, registrationNumber, city);
        this.networkContacts.add(contact);
        touch(clock);
        return contact;
    }

    public void removeNetworkContact(String contactId, Clock clock) {
        boolean removed = this.networkContacts.removeIf(contact -> contact.id().equals(contactId));
        if (!removed) {
            throw ProfileException.of(ProfileErrorCode.PROFILE_NOT_FOUND,
                    "Aucun contact de reseau ne correspond a l'identifiant " + contactId);
        }
        touch(clock);
    }

    /** Dossier publiable : identite complete, specialite declaree et informations de consultation presentes. */
    public boolean isPublishable() {
        return identity != null
                && identity.mainSpecialty() != null
                && !identity.mainSpecialty().isBlank()
                && practiceInformation != null
                && practiceInformation.sector() != null;
    }

    /** Efface les donnees personnelles du dossier (droit a l'oubli). */
    public void erase(Clock clock) {
        this.identity = null;
        this.practiceInformation = PracticeInformation.defaults();
        this.management = null;
        this.visibility = VisibilityProfile.empty();
        this.networkContacts = new ArrayList<>();
        touch(clock);
    }

    // ------------------------------------------------------------------

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    private static List<String> copyOf(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value.trim());
            }
        }
        return List.copyOf(cleaned);
    }

    /** Ensemble deduplique conservant l'ordre de declaration. */
    public static Set<PaymentMethod> paymentMethods(PaymentMethod... methods) {
        return new LinkedHashSet<>(List.of(methods));
    }
}
