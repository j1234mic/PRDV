package com.prdv.adapters.out.persistence.profile;

import com.prdv.profile.application.port.out.PatientProfileRepository;
import com.prdv.profile.domain.model.PatientProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaPatientProfileRepository implements PatientProfileRepository {

    private final PatientProfileJpaRepository jpa;

    public JpaPatientProfileRepository(PatientProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PatientProfile save(PatientProfile profile) {
        PatientProfileEntity e = new PatientProfileEntity();
        e.setId(profile.id());
        e.setUserId(profile.userId());
        e.setFirstName(profile.firstName());
        e.setLastName(profile.lastName());
        e.setBirthDate(profile.birthDate());
        e.setGender(profile.gender());
        e.setPhone(profile.phone());
        e.setAddressLine(profile.addressLine());
        e.setPostalCode(profile.postalCode());
        e.setCity(profile.city());
        e.setCountry(profile.country());
        e.setSocialSecurityNumber(profile.socialSecurityNumber());
        e.setMutualInsurance(profile.mutualInsurance());
        e.setAllergies(profile.allergies());
        e.setChronicConditions(profile.chronicConditions());
        e.setEmergencyContactName(profile.emergencyContactName());
        e.setEmergencyContactPhone(profile.emergencyContactPhone());
        PatientProfileEntity saved = jpa.save(e);
        if (profile.id() == null) {
            profile.assignId(saved.getId());
        }
        return profile;
    }

    @Override
    public Optional<PatientProfile> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(JpaPatientProfileRepository::toDomain);
    }

    private static PatientProfile toDomain(PatientProfileEntity e) {
        return new PatientProfile(e.getId(), e.getUserId(), e.getFirstName(), e.getLastName(), e.getBirthDate(),
                e.getGender(), e.getPhone(), e.getAddressLine(), e.getPostalCode(), e.getCity(), e.getCountry(),
                e.getSocialSecurityNumber(), e.getMutualInsurance(), e.getAllergies(), e.getChronicConditions(),
                e.getEmergencyContactName(), e.getEmergencyContactPhone());
    }
}
