package com.prdv.profile.domain.model;

import com.prdv.shared.exception.ValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Profil patient (module 2.1) - AGREGAT.
 * Les donnees sensibles (ID card, carte vitale) ne vivent PAS ici : seules les
 * referenceances securisees du vault chiffré (adaptateur) transitent -> minimisation RGPD.
 * Le numero de secu est stocke, masque a l'affichage (DTO) et jamais loggue.
 */
public final class PatientProfile {

    private Long id;
    private final Long userId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String gender;
    private String phone;
    private String addressLine;
    private String postalCode;
    private String city;
    private String country;
    private String socialSecurityNumber;
    private String mutualInsurance;
    private List<String> allergies;
    private List<String> chronicConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;

    public PatientProfile(Long id, Long userId, String firstName, String lastName, LocalDate birthDate,
                          String gender, String phone, String addressLine, String postalCode, String city,
                          String country, String socialSecurityNumber, String mutualInsurance,
                          List<String> allergies, List<String> chronicConditions,
                          String emergencyContactName, String emergencyContactPhone) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.phone = phone;
        this.addressLine = addressLine;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
        this.socialSecurityNumber = socialSecurityNumber;
        this.mutualInsurance = mutualInsurance;
        this.allergies = allergies == null ? List.of() : List.copyOf(allergies);
        this.chronicConditions = chronicConditions == null ? List.of() : List.copyOf(chronicConditions);
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        validate();
    }

    private void validate() {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new ValidationException("Nom et prenom requis");
        }
        if (birthDate == null) {
            throw new ValidationException("Date de naissance requise");
        }
        if (birthDate.isAfter(LocalDate.now()) || ChronoUnit.YEARS.between(birthDate, LocalDate.now()) > 120) {
            throw new ValidationException("Date de naissance invraisemblable");
        }
        if (socialSecurityNumber != null && !socialSecurityNumber.isBlank() && !isFrenchNir(socialSecurityNumber)) {
            throw new ValidationException("Numero de securite sociale invalide (15 chiffres attendus)");
        }
    }

    /** NIR francais : 13 chiffres + cle de 2 chiffres, cle = 97 - (nombre a 13 chiffres mod 97). */
    private static boolean isFrenchNir(String nir) {
        String digits = nir.replaceAll("\\s", "");
        if (!digits.matches("\\d{15}")) {
            return false;
        }
        long base = Long.parseLong(digits.substring(0, 13));
        long key = 97 - (base % 97);
        return Integer.parseInt(digits.substring(13)) == key;
    }

    public boolean isMinor() {
        return ChronoUnit.YEARS.between(birthDate, LocalDate.now()) < 18;
    }

    public boolean isAllergicTo(String allergen) {
        return allergies.stream().anyMatch(a -> a.equalsIgnoreCase(allergen));
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Identifiant deja affecte");
        }
        this.id = id;
    }

    /** Mise a jour du dossier medical leger (allergies/chroniques) - invariant revalide par le constructeur. */
    public PatientProfile withMedicalFacts(List<String> newAllergies, List<String> newChronic) {
        return new PatientProfile(id, userId, firstName, lastName, birthDate, gender, phone,
                addressLine, postalCode, city, country, socialSecurityNumber, mutualInsurance,
                newAllergies, newChronic, emergencyContactName, emergencyContactPhone);
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public String firstName() { return firstName; }
    public String lastName() { return lastName; }
    public LocalDate birthDate() { return birthDate; }
    public String gender() { return gender; }
    public String phone() { return phone; }
    public String addressLine() { return addressLine; }
    public String postalCode() { return postalCode; }
    public String city() { return city; }
    public String country() { return country; }
    public String socialSecurityNumber() { return socialSecurityNumber; }
    public String mutualInsurance() { return mutualInsurance; }
    public List<String> allergies() { return allergies; }
    public List<String> chronicConditions() { return chronicConditions; }
    public String emergencyContactName() { return emergencyContactName; }
    public String emergencyContactPhone() { return emergencyContactPhone; }
}
