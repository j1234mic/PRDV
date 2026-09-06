package com.prdv.profile.application.service;

import com.prdv.profile.application.port.in.CreatePatientProfileUseCase;
import com.prdv.profile.application.port.in.UpdateMedicalFactsUseCase;
import com.prdv.profile.application.port.out.PatientProfileRepository;
import com.prdv.profile.domain.model.PatientProfile;
import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.NotFoundException;

import java.util.List;

/** SRP : un seul handler porte les 2 cas d'usage du profil patient car ils partagent le meme agregat. */
public final class PatientProfileHandler implements CreatePatientProfileUseCase, UpdateMedicalFactsUseCase {

    private final PatientProfileRepository repository;

    public PatientProfileHandler(PatientProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public PatientProfile create(Command command) {
        repository.findByUserId(command.userId()).ifPresent(p -> {
            throw new ConflictException("Profil patient deja existant");
        });
        PatientProfile profile = new PatientProfile(null, command.userId(), command.firstName(),
                command.lastName(), command.birthDate(), command.gender(), command.phone(),
                command.addressLine(), command.postalCode(), command.city(), command.country(),
                command.socialSecurityNumber(), command.mutualInsurance(), command.allergies(),
                command.chronicConditions(), command.emergencyContactName(), command.emergencyContactPhone());
        return repository.save(profile);
    }

    @Override
    public PatientProfile update(Long userId, List<String> allergies, List<String> chronicConditions) {
        PatientProfile current = repository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profil patient introuvable"));
        return repository.save(current.withMedicalFacts(allergies, chronicConditions));
    }
}
