package com.prdv.profile.application.port.in;

import com.prdv.profile.domain.model.DoctorProfile;

import java.util.List;

public interface UpdateDoctorProfileUseCase {

    record Command(Long doctorUserId, String fullName, String specialty, String description, int sector,
                   int consultationFeeCents, List<String> languages,
                   List<CreateDoctorProfileUseCase.PracticeLocationDto> locations) { }

    DoctorProfile update(Command command);
}
