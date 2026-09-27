package com.prdv.rdv.profile.domain.model.identity;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Agregat racine « Identite patient » (module 2.1 — Informations Personnelles).
 *
 * <p>Porte l'etat civil complet, les contacts multiples (fixe, mobile,
 * professionnel), les adresses multiples (domicile, travail), la personne a
 * prevenir en cas d'urgence, le medecin traitant declare, le numero de
 * securite sociale (tokenise), les mutuelles principale et complementaire, la
 * photo de profil, la piece d'identite, la carte Vitale (scan ou lecture NFC)
 * et le lien vers le Dossier Medical Partage (DMP).
 *
 * <p>Regles metier encapsulees dans l'agregat (modele riche) : un seul contact
 * prefere par type, une seule adresse par defaut, au plus une mutuelle de
 * chaque nature, aucune donnee sensible en clair.
 */
@Getter
@Setter
public class PatientIdentity {

    // ------------------------------------------------------------------
    // Enumerations metier
    // ------------------------------------------------------------------

    public enum Gender { M, F, OTHER, UNSPECIFIED }

    public enum Civility { MR, MME, MLLE, MXT, DR, PR }

    public enum MaritalStatus { SINGLE, MARRIED, CIVIL_PARTNERSHIP, DIVORCED, WIDOWED, SEPARATED }

    public enum ContactType { MOBILE, HOME, WORK, FAX, EMAIL }

    public enum AddressType { HOME, WORK, OTHER }

    public enum InsuranceType {
        /** Regime obligatoire (CPAM, MSA...). */
        PRINCIPAL,
        /** Mutuelle / complementaire sante. */
        COMPLEMENTARY,
        UNIVERSAL_COVERAGE,
        STATE_MEDICAL_AID,
        PRIVATE,
        FOREIGN
    }

    /** Provenance des donnees de la carte Vitale : lecture NFC, scan ou saisie. */
    public enum VitaleReadMode { NFC, SCAN, MANUAL }

    // ------------------------------------------------------------------
    // Value Objects
    // ------------------------------------------------------------------

    /** Etat civil complet : le bloc modifiable en une seule operation atomique. */
    public record CivilStatus(Civility civility,
                              String firstName,
                              String birthName,
                              String lastName,
                              String preferredName,
                              LocalDate birthDate,
                              String birthPlace,
                              String birthCountry,
                              Gender gender,
                              MaritalStatus maritalStatus,
                              String nationality) {

        public CivilStatus {
            if (firstName == null || firstName.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le prenom est obligatoire");
            }
            if (lastName == null || lastName.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le nom est obligatoire");
            }
            firstName = firstName.trim();
            lastName = lastName.trim();
            if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La date de naissance ne peut pas etre future");
            }
        }
    }

    /** Coordonnee : telephone (fixe, mobile, pro, fax) ou email. */
    public record ContactPoint(ContactType type, String value, boolean verified, boolean preferred) {

        private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9\\s().-]{5,24}$");
        private static final Pattern EMAIL_PATTERN =
                Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        public ContactPoint {
            if (type == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le type de contact est obligatoire");
            }
            if (value == null || value.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La valeur du contact est obligatoire");
            }
            value = value.trim();
            boolean valid = type == ContactType.EMAIL
                    ? EMAIL_PATTERN.matcher(value.toLowerCase(Locale.ROOT)).matches()
                    : PHONE_PATTERN.matcher(value).matches();
            if (!valid) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Format de contact invalide pour " + type + " : " + value);
            }
            if (type == ContactType.EMAIL) {
                value = value.toLowerCase(Locale.ROOT);
            }
        }

        public ContactPoint withPreferred(boolean newPreferred) {
            return new ContactPoint(type, value, verified, newPreferred);
        }
    }

    /** Adresse postale : domicile, travail ou autre, avec geolocalisation optionnelle. */
    public record PostalAddress(AddressType type,
                                String line1,
                                String line2,
                                String postalCode,
                                String city,
                                String country,
                                Double latitude,
                                Double longitude,
                                boolean isDefault) {

        public PostalAddress {
            if (type == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le type d'adresse est obligatoire");
            }
            if (line1 == null || line1.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "La ligne 1 d'adresse est obligatoire");
            }
            if (city == null || city.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "La ville est obligatoire");
            }
            line1 = line1.trim();
            city = city.trim();
            if (latitude != null && (latitude < -90 || latitude > 90)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Latitude invalide : " + latitude);
            }
            if (longitude != null && (longitude < -180 || longitude > 180)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Longitude invalide : " + longitude);
            }
        }

        public PostalAddress withDefault(boolean newDefault) {
            return new PostalAddress(type, line1, line2, postalCode, city, country,
                    latitude, longitude, newDefault);
        }

        public boolean isGeolocated() {
            return latitude != null && longitude != null;
        }
    }

    /** Personne a prevenir en cas d'urgence. */
    public record EmergencyContact(String firstName,
                                   String lastName,
                                   String relationship,
                                   String phone,
                                   String email) {

        public EmergencyContact {
            if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Nom et prenom de la personne a prevenir sont obligatoires");
            }
            if (phone == null || phone.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le telephone de la personne a prevenir est obligatoire");
            }
            firstName = firstName.trim();
            lastName = lastName.trim();
        }

        public String fullName() {
            return firstName + " " + lastName;
        }
    }

    /** Medecin traitant declare (parcours de soins coordonnes). */
    public record TreatingPhysician(String firstName,
                                    String lastName,
                                    String rppsNumber,
                                    String phone,
                                    String email,
                                    boolean declaredToInsurance) {

        public TreatingPhysician {
            if (lastName == null || lastName.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le nom du medecin traitant est obligatoire");
            }
            lastName = lastName.trim();
        }

        public String fullName() {
            return (firstName == null || firstName.isBlank() ? "" : firstName + " ") + lastName;
        }
    }

    /** Couverture sociale : regime principal ou mutuelle complementaire. */
    public record HealthInsurance(InsuranceType type,
                                  String organization,
                                  String memberNumber,
                                  String contractReference,
                                  LocalDate validUntil) {

        public HealthInsurance {
            if (type == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le type de couverture est obligatoire");
            }
            if (organization == null || organization.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "L'organisme de rattachement est obligatoire");
            }
            organization = organization.trim();
        }

        public boolean isComplementary() {
            return type != InsuranceType.PRINCIPAL;
        }
    }

    /** Carte Vitale : lecture NFC (donnees structurees) ou scan (document). */
    public record VitaleCard(String nir,
                             String cardVersion,
                             LocalDate issuedOn,
                             LocalDate expiresOn,
                             VitaleReadMode readMode,
                             String scanStorageKey,
                             Instant readAt) {

        public VitaleCard {
            if (readMode == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le mode de lecture de la carte Vitale est obligatoire");
            }
            if ((nir == null || nir.isBlank()) && (scanStorageKey == null || scanStorageKey.isBlank())) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La carte Vitale exige soit un NIR (lecture NFC) soit un scan");
            }
            if (issuedOn != null && expiresOn != null && expiresOn.isBefore(issuedOn)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La date d'expiration de la carte Vitale precede sa date d'emission");
            }
        }

        public boolean isExpired(LocalDate today) {
            return expiresOn != null && expiresOn.isBefore(today);
        }
    }

    /** Rattachement au Dossier Medical Partage national (DMP). */
    public record DmpAccount(String dmpIdentifier,
                             boolean linked,
                             boolean sharingEnabled,
                             Instant linkedAt,
                             Instant lastSyncAt) {

        public DmpAccount {
            if (linked && (dmpIdentifier == null || dmpIdentifier.isBlank())) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Un DMP rattache exige un identifiant DMP");
            }
        }

        public static DmpAccount notLinked() {
            return new DmpAccount(null, false, false, null, null);
        }
    }

    // ------------------------------------------------------------------
    // Etat de l'agregat
    // ------------------------------------------------------------------

    private static final int MAX_CONTACTS = 10;
    private static final int MAX_ADDRESSES = 6;

    private Long id;
    private Long userId;

    private CivilStatus civilStatus;

    private Set<ContactPoint> contacts = new LinkedHashSet<>();
    private Set<PostalAddress> addresses = new LinkedHashSet<>();

    private EmergencyContact emergencyContact;
    private TreatingPhysician treatingPhysician;

    /** Jeton non reversible du NIR : la valeur brute n'est jamais persistee. */
    private String socialSecurityNumberToken;
    private String maskedSocialSecurityNumber;

    private String photoStorageKey;
    private String identityDocumentStorageKey;
    private String identityDocumentToken;

    private VitaleCard vitaleCard;
    private HealthInsurance primaryInsurance;
    private HealthInsurance complementaryInsurance;
    private DmpAccount dmpAccount = DmpAccount.notLinked();

    private Instant createdAt;
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Fabrique
    // ------------------------------------------------------------------

    public static PatientIdentity create(Long userId, CivilStatus civilStatus, Clock clock) {
        if (userId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "L'identite patient doit etre rattachee a un utilisateur");
        }
        PatientIdentity identity = new PatientIdentity();
        identity.userId = userId;
        identity.civilStatus = civilStatus;
        identity.createdAt = clock.instant();
        identity.updatedAt = identity.createdAt;
        return identity;
    }

    // ------------------------------------------------------------------
    // Comportements : etat civil
    // ------------------------------------------------------------------

    public void updateCivilStatus(CivilStatus newCivilStatus, Clock clock) {
        this.civilStatus = newCivilStatus;
        touch(clock);
    }

    // ------------------------------------------------------------------
    // Comportements : contacts et adresses
    // ------------------------------------------------------------------

    /** Remplace la liste des contacts en garantissant un seul prefere par type. */
    public void replaceContacts(List<ContactPoint> newContacts, Clock clock) {
        if (newContacts == null) {
            newContacts = List.of();
        }
        if (newContacts.size() > MAX_CONTACTS) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Trop de contacts (maximum " + MAX_CONTACTS + ")");
        }
        Set<ContactPoint> normalized = new LinkedHashSet<>();
        for (ContactPoint contact : newContacts) {
            ContactPoint candidate = contact;
            for (ContactPoint existing : normalized) {
                if (existing.type() == candidate.type() && existing.value().equals(candidate.value())) {
                    throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                            "Contact deja present : " + candidate.type() + " " + candidate.value());
                }
            }
            ContactType candidateType = candidate.type();
            boolean alreadyPreferred = normalized.stream()
                    .anyMatch(existing -> existing.type() == candidateType && existing.preferred());
            if (candidate.preferred() && alreadyPreferred) {
                candidate = candidate.withPreferred(false);
            }
            normalized.add(candidate);
        }
        this.contacts = normalized;
        touch(clock);
    }

    public Optional<ContactPoint> preferredContact(ContactType type) {
        return contacts.stream()
                .filter(contact -> contact.type() == type)
                .reduce((first, second) -> second.preferred() ? second : first);
    }

    /** Remplace les adresses en garantissant une seule adresse par defaut. */
    public void replaceAddresses(List<PostalAddress> newAddresses, Clock clock) {
        if (newAddresses == null) {
            newAddresses = List.of();
        }
        if (newAddresses.size() > MAX_ADDRESSES) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Trop d'adresses (maximum " + MAX_ADDRESSES + ")");
        }
        List<PostalAddress> normalized = new ArrayList<>();
        boolean defaultAssigned = false;
        for (PostalAddress address : newAddresses) {
            PostalAddress candidate = address;
            if (candidate.isDefault() && defaultAssigned) {
                candidate = candidate.withDefault(false);
            }
            if (candidate.isDefault()) {
                defaultAssigned = true;
            }
            normalized.add(candidate);
        }
        if (normalized.size() == 1) {
            PostalAddress only = normalized.get(0);
            normalized.set(0, only.withDefault(true));
        }
        this.addresses = new LinkedHashSet<>(normalized);
        touch(clock);
    }

    public Optional<PostalAddress> defaultAddress() {
        return addresses.stream().filter(PostalAddress::isDefault).findFirst()
                .or(() -> addresses.stream().filter(a -> a.type() == AddressType.HOME).findFirst())
                .or(() -> addresses.stream().findFirst());
    }

    public void updateEmergencyContact(EmergencyContact contact, Clock clock) {
        this.emergencyContact = contact;
        touch(clock);
    }

    public void declareTreatingPhysician(TreatingPhysician physician, Clock clock) {
        this.treatingPhysician = physician;
        touch(clock);
    }

    // ------------------------------------------------------------------
    // Comportements : donnees sensibles
    // ------------------------------------------------------------------

    /** Enregistre le NIR sous forme tokenisee + valeur masquee (jamais en clair). */
    public void registerSocialSecurityNumber(String token, String maskedValue, Clock clock) {
        if (token == null || token.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le jeton de numero de securite sociale est obligatoire");
        }
        this.socialSecurityNumberToken = token;
        this.maskedSocialSecurityNumber = maskedValue;
        touch(clock);
    }

    public void attachPhoto(String storageKey, Clock clock) {
        this.photoStorageKey = requireStorageKey(storageKey, "photo de profil");
        touch(clock);
    }

    public void attachIdentityDocument(String storageKey, String token, Clock clock) {
        this.identityDocumentStorageKey = requireStorageKey(storageKey, "piece d'identite");
        this.identityDocumentToken = token;
        touch(clock);
    }

    public void registerVitaleCard(VitaleCard card, Clock clock) {
        this.vitaleCard = card;
        touch(clock);
    }

    /** Affecte la couverture au bon emplacement selon sa nature (principale / complementaire). */
    public void updateInsurance(HealthInsurance insurance, Clock clock) {
        if (insurance == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "La couverture est obligatoire");
        }
        if (insurance.type() == InsuranceType.PRINCIPAL) {
            this.primaryInsurance = insurance;
        } else {
            this.complementaryInsurance = insurance;
        }
        touch(clock);
    }

    // ------------------------------------------------------------------
    // Comportements : DMP
    // ------------------------------------------------------------------

    public void linkDmp(String dmpIdentifier, boolean sharingEnabled, Clock clock) {
        this.dmpAccount = new DmpAccount(dmpIdentifier, true, sharingEnabled, clock.instant(), null);
        touch(clock);
    }

    public void unlinkDmp(Clock clock) {
        this.dmpAccount = DmpAccount.notLinked();
        touch(clock);
    }

    public void updateDmpSharing(boolean sharingEnabled, Clock clock) {
        DmpAccount current = dmpAccount == null ? DmpAccount.notLinked() : dmpAccount;
        this.dmpAccount = new DmpAccount(current.dmpIdentifier(), current.linked(), sharingEnabled,
                current.linkedAt(), current.lastSyncAt());
        touch(clock);
    }

    public void markDmpSynced(Clock clock) {
        DmpAccount current = dmpAccount == null ? DmpAccount.notLinked() : dmpAccount;
        if (!current.linked()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Aucun DMP rattache : synchronisation impossible");
        }
        this.dmpAccount = new DmpAccount(current.dmpIdentifier(), true, current.sharingEnabled(),
                current.linkedAt(), clock.instant());
        touch(clock);
    }

    // ------------------------------------------------------------------
    // RGPD
    // ------------------------------------------------------------------

    /** Efface les donnees personnelles du profil (droit a l'oubli). */
    public void erase(Clock clock) {
        this.civilStatus = new CivilStatus(null, "ANONYMISE", null, "ANONYMISE", null, null,
                null, null, null, null, null);
        this.contacts = new LinkedHashSet<>();
        this.addresses = new LinkedHashSet<>();
        this.emergencyContact = null;
        this.treatingPhysician = null;
        this.socialSecurityNumberToken = null;
        this.maskedSocialSecurityNumber = null;
        this.photoStorageKey = null;
        this.identityDocumentStorageKey = null;
        this.identityDocumentToken = null;
        this.vitaleCard = null;
        this.primaryInsurance = null;
        this.complementaryInsurance = null;
        this.dmpAccount = DmpAccount.notLinked();
        touch(clock);
    }

    // ------------------------------------------------------------------

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    private static String requireStorageKey(String storageKey, String label) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La cle de stockage de la " + label + " est obligatoire");
        }
        return storageKey;
    }
}
