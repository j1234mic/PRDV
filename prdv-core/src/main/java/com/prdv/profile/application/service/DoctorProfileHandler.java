package com.prdv.profile.application.service;

import com.prdv.profile.application.port.in.CreateDoctorProfileUseCase;
import com.prdv.profile.application.port.in.UpdateDoctorProfileUseCase;
import com.prdv.profile.application.port.out.DoctorProfileRepository;
import com.prdv.profile.application.port.out.MedicalRegistryGateway;
import com.prdv.profile.domain.model.DoctorProfile;
import com.prdv.profile.domain.model.PracticeLocation;
import com.prdv.profile.domain.model.RppsNumber;
import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.NotFoundException;
import com.prdv.shared.exception.ValidationException;

import java.util.List;

/**
 * Creation d'un profil praticien avec "Vérification RPPS/ADELI automatique" :
 * 1) algorithme de cle RPPS (dans RppsNumber - Value Object auto-validant)
 * 2) lookup annuaire officiel (port gateway)
 * 3) passage en verification manuelle admin.
 */
public final class DoctorProfileHandler implements CreateDoctorProfileUseCase, UpdateDoctorProfileUseCase {

    private final DoctorProfileRepository doctors;
    private final MedicalRegistryGateway registry;

    public DoctorProfileHandler(DoctorProfileRepository doctors, MedicalRegistryGateway registry) {
        this.doctors = doctors;
        this.registry = registry;
    }

    @Override
    public DoctorProfile create(Command command) {
        doctors.findByUserId(command.userId()).ifPresent(p -> {
            throw new ConflictException("Profil praticien deja existant");
        });
        RppsNumber rpps = RppsNumber.of(command.rpps());
        doctors.findByRpps(rpps.value()).ifPresent(p -> {
            throw new ConflictException("Numero RPPS deja utilise sur la plateforme");
        });

        MedicalRegistryGateway.RegistryEntry entry = registry.findByRpps(rpps.value())
                .orElseThrow(() -> new ValidationException("Numero RPPS inconnu de l'annuaire de l'Ordre"));
        if (!entry.active()) {
            throw new ValidationException("Inscription Ordre non active");
        }
        if (command.fullName() == null || !command.fullName().toLowerCase().contains(lastNameOf(entry.legalName()))) {
            throw new ValidationException("Le nom declare ne correspond pas a l'annuaire Ordre");
        }

        DoctorProfile profile = new DoctorProfile(null, command.userId(), command.fullName(), rpps,
                command.specialty(), command.subSpecialties(), command.description(), command.sector(),
                command.consultationFeeCents(), command.languages(), toLocations(command.locations()),
                com.prdv.profile.domain.model.DoctorVerificationStatus.PENDING_VALIDATION, null);
        return doctors.save(profile);
    }

    @Override
    public DoctorProfile update(Command command) {
        DoctorProfile profile = doctors.findByUserId(command.doctorUserId())
                .orElseThrow(() -> new NotFoundException("Profil praticien introuvable"));
        profile.edit(command.fullName(), command.specialty(), command.description(), command.sector(),
                command.consultationFeeCents(), command.languages(), toLocations(command.locations()));
        return doctors.save(profile);
    }

    private static List<PracticeLocation> toLocations(List<CreateDoctorProfileUseCase.PracticeLocationDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(l -> new PracticeLocation(l.name(), l.address(), l.postalCode(), l.city(), l.phone()))
                .toList();
    }

    private static String lastNameOf(String legalName) {
        if (legalName == null || legalName.isBlank()) {
            return "";
        }
        String[] parts = legalName.trim().split("\\s+");
        return parts[parts.length - 1].toLowerCase();
    }
}
