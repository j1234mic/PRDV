package com.prdv.profile.application.port.in;

import com.prdv.profile.domain.model.DoctorProfile;

import java.util.List;

public interface CreateDoctorProfileUseCase {

    record Command(Long userId, String fullName, String rpps, String specialty, List<String> subSpecialties,
                   String description, int sector, int consultationFeeCents, List<String> languages,
                   List<PracticeLocationDto> locations) { }

    record PracticeLocationDto(String name, String address, String postalCode, String city, String phone) { }

    DoctorProfile create(Command command);
}
