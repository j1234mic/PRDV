package com.prdv.profile.application.port.in;

import com.prdv.profile.domain.model.PatientProfile;

import java.time.LocalDate;
import java.util.List;

public interface CreatePatientProfileUseCase {

    record Command(Long userId, String firstName, String lastName, LocalDate birthDate, String gender,
                   String phone, String addressLine, String postalCode, String city, String country,
                   String socialSecurityNumber, String mutualInsurance, List<String> allergies,
                   List<String> chronicConditions, String emergencyContactName, String emergencyContactPhone) { }

    PatientProfile create(Command command);
}
