package com.prdv.profile.application.port.in;

import com.prdv.profile.domain.model.PatientProfile;

import java.util.List;

public interface UpdateMedicalFactsUseCase {
    PatientProfile update(Long userId, List<String> allergies, List<String> chronicConditions);
}
