package com.prdv.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record PatientProfileRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull LocalDate birthDate,
            String gender,
            String phone,
            String addressLine,
            String postalCode,
            String city,
            String country,
            String socialSecurityNumber,
            String mutualInsurance,
            List<String> allergies,
            List<String> chronicConditions,
            String emergencyContactName,
            String emergencyContactPhone) {
    }

    /** Le numero de securite sociale est MASQUE a la sortie (minimisation RGPD). */
    public record PatientProfileResponse(Long userId, String firstName, String lastName, LocalDate birthDate,
                                         String gender, String phone, String city, String maskedSsn,
                                         String mutualInsurance, List<String> allergies,
                                         List<String> chronicConditions, boolean minor,
                                         String emergencyContactName) {
    }

    public record MedicalFactsRequest(List<String> allergies, List<String> chronicConditions) {
    }

    public record DoctorProfileRequest(
            @NotBlank String fullName,
            @NotBlank String rpps,
            @NotBlank String specialty,
            List<String> subSpecialties,
            String description,
            int sector,
            int consultationFeeCents,
            List<String> languages,
            List<PracticeLocationDto> locations) {
    }

    public record PracticeLocationDto(String name, String address, String postalCode, String city, String phone) {
    }

    public record DoctorProfileResponse(Long userId, String fullName, String specialty, List<String> subSpecialties,
                                        String description, int sector, int consultationFeeCents,
                                        List<String> languages, List<PracticeLocationDto> locations,
                                        String verificationStatus, String rejectionReason) {
    }

    public record RejectRequest(String reason) {
    }
}
