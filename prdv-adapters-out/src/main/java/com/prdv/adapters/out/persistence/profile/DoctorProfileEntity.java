package com.prdv.adapters.out.persistence.profile;

import com.prdv.adapters.out.persistence.json.PracticeLocationListConverter;
import com.prdv.adapters.out.persistence.json.StringListConverter;
import com.prdv.profile.domain.model.DoctorVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "doctor_profiles", indexes = @Index(name = "idx_doctor_specialty", columnList = "specialty"))
public class DoctorProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 11)
    private String rpps;

    @Column(nullable = false, length = 120)
    private String specialty;

    @Convert(converter = StringListConverter.class)
    @Column(name = "sub_specialties", columnDefinition = "TEXT")
    private List<String> subSpecialties;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int sector;

    @Column(name = "consultation_fee_cents", nullable = false)
    private int consultationFeeCents;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> languages;

    @Convert(converter = PracticeLocationListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<com.prdv.profile.domain.model.PracticeLocation> locations;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private DoctorVerificationStatus verificationStatus;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRpps() { return rpps; }
    public void setRpps(String rpps) { this.rpps = rpps; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public List<String> getSubSpecialties() { return subSpecialties; }
    public void setSubSpecialties(List<String> s) { this.subSpecialties = s; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getSector() { return sector; }
    public void setSector(int sector) { this.sector = sector; }
    public int getConsultationFeeCents() { return consultationFeeCents; }
    public void setConsultationFeeCents(int c) { this.consultationFeeCents = c; }
    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }
    public List<com.prdv.profile.domain.model.PracticeLocation> getLocations() { return locations; }
    public void setLocations(List<com.prdv.profile.domain.model.PracticeLocation> l) { this.locations = l; }
    public DoctorVerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(DoctorVerificationStatus v) { this.verificationStatus = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String r) { this.rejectionReason = r; }
}
