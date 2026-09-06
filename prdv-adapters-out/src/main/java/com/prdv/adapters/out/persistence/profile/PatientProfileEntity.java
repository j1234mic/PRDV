package com.prdv.adapters.out.persistence.profile;

import com.prdv.adapters.out.persistence.json.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "patient_profiles")
public class PatientProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 20)
    private String gender;

    @Column(length = 32)
    private String phone;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "postal_code", length = 12)
    private String postalCode;

    @Column(length = 80)
    private String city;

    @Column(length = 60)
    private String country;

    @Column(name = "social_security_number", length = 32)
    private String socialSecurityNumber;

    @Column(name = "mutual_insurance", length = 120)
    private String mutualInsurance;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> allergies;

    @Convert(converter = StringListConverter.class)
    @Column(name = "chronic_conditions", columnDefinition = "TEXT")
    private List<String> chronicConditions;

    @Column(name = "emergency_contact_name", length = 120)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 32)
    private String emergencyContactPhone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getSocialSecurityNumber() { return socialSecurityNumber; }
    public void setSocialSecurityNumber(String s) { this.socialSecurityNumber = s; }
    public String getMutualInsurance() { return mutualInsurance; }
    public void setMutualInsurance(String mutualInsurance) { this.mutualInsurance = mutualInsurance; }
    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }
    public List<String> getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(List<String> chronicConditions) { this.chronicConditions = chronicConditions; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String n) { this.emergencyContactName = n; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String p) { this.emergencyContactPhone = p; }
}
