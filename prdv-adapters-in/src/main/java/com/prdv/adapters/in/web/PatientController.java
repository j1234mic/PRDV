package com.prdv.adapters.in.web;

import com.prdv.adapters.in.security.AuthenticatedUser;
import com.prdv.adapters.in.web.dto.ProfileDtos;
import com.prdv.adapters.in.web.dto.ProfileDtos.DoctorProfileRequest;
import com.prdv.adapters.in.web.dto.ProfileDtos.DoctorProfileResponse;
import com.prdv.adapters.in.web.dto.ProfileDtos.MedicalFactsRequest;
import com.prdv.adapters.in.web.dto.ProfileDtos.PatientProfileRequest;
import com.prdv.adapters.in.web.dto.ProfileDtos.PatientProfileResponse;
import com.prdv.profile.application.port.in.CreateDoctorProfileUseCase;
import com.prdv.profile.application.port.in.CreatePatientProfileUseCase;
import com.prdv.profile.application.port.in.UpdateDoctorProfileUseCase;
import com.prdv.profile.application.port.in.UpdateMedicalFactsUseCase;
import com.prdv.profile.application.port.out.DoctorProfileRepository;
import com.prdv.profile.application.port.out.PatientProfileRepository;
import com.prdv.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Profils PATIENT et praticien (cote utilisateur connecte).
 * NB : le userId vient TOUJOURS du jeton (jamais du body) -> pas d'escalade de privilege.
 */
@RestController
@RequestMapping("/api")
public class PatientController {

    private final CreatePatientProfileUseCase createPatient;
    private final UpdateMedicalFactsUseCase updateFacts;
    private final CreateDoctorProfileUseCase createDoctor;
    private final UpdateDoctorProfileUseCase updateDoctor;
    private final PatientProfileRepository patients;
    private final DoctorProfileRepository doctors;

    public PatientController(CreatePatientProfileUseCase createPatient, UpdateMedicalFactsUseCase updateFacts,
                             CreateDoctorProfileUseCase createDoctor, UpdateDoctorProfileUseCase updateDoctor,
                             PatientProfileRepository patients, DoctorProfileRepository doctors) {
        this.createPatient = createPatient;
        this.updateFacts = updateFacts;
        this.createDoctor = createDoctor;
        this.updateDoctor = updateDoctor;
        this.patients = patients;
        this.doctors = doctors;
    }

    @GetMapping("/patients/me")
    public PatientProfileResponse myPatientProfile(@AuthenticationPrincipal AuthenticatedUser me) {
        return DtoMapper.toResponse(patients.findByUserId(me.id())
                .orElseThrow(() -> new NotFoundException("Aucun profil patient pour ce compte")));
    }

    @PostMapping("/patients/me")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    public PatientProfileResponse createMyProfile(@AuthenticationPrincipal AuthenticatedUser me,
                                                  @Valid @RequestBody PatientProfileRequest request) {
        return DtoMapper.toResponse(createPatient.create(toCommand(me.id(), request)));
    }

    @PatchMapping("/patients/me/medical-facts")
    @PreAuthorize("hasRole('PATIENT')")
    public PatientProfileResponse updateMedicalFacts(@AuthenticationPrincipal AuthenticatedUser me,
                                                     @RequestBody MedicalFactsRequest request) {
        return DtoMapper.toResponse(updateFacts.update(me.id(), request.allergies(), request.chronicConditions()));
    }

    @GetMapping("/doctors/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorProfileResponse myDoctorProfile(@AuthenticationPrincipal AuthenticatedUser me) {
        return DtoMapper.toResponse(doctors.findByUserId(me.id())
                .orElseThrow(() -> new NotFoundException("Aucun profil praticien pour ce compte")));
    }

    @PostMapping("/doctors/me")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorProfileResponse createMyDoctorProfile(@AuthenticationPrincipal AuthenticatedUser me,
                                                       @Valid @RequestBody DoctorProfileRequest request) {
        return DtoMapper.toResponse(createDoctor.create(toDoctorCommand(me.id(), request)));
    }

    @PutMapping("/doctors/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorProfileResponse updateMyDoctorProfile(@AuthenticationPrincipal AuthenticatedUser me,
                                                       @Valid @RequestBody DoctorProfileRequest request) {
        return DtoMapper.toResponse(updateDoctor.update(new UpdateDoctorProfileUseCase.Command(
                me.id(), request.fullName(), request.specialty(), request.description(), request.sector(),
                request.consultationFeeCents(), request.languages(), toLocations(request.locations()))));
    }

    // ----- conversions request -> command ----------------------------------------

    private static CreatePatientProfileUseCase.Command toCommand(Long userId, PatientProfileRequest r) {
        return new CreatePatientProfileUseCase.Command(userId, r.firstName(), r.lastName(), r.birthDate(),
                r.gender(), r.phone(), r.addressLine(), r.postalCode(), r.city(), r.country(),
                r.socialSecurityNumber(), r.mutualInsurance(), r.allergies(), r.chronicConditions(),
                r.emergencyContactName(), r.emergencyContactPhone());
    }

    private static CreateDoctorProfileUseCase.Command toDoctorCommand(Long userId, DoctorProfileRequest r) {
        return new CreateDoctorProfileUseCase.Command(userId, r.fullName(), r.rpps(), r.specialty(),
                r.subSpecialties(), r.description(), r.sector(), r.consultationFeeCents(), r.languages(),
                toLocations(r.locations()));
    }

    private static List<CreateDoctorProfileUseCase.PracticeLocationDto> toLocations(
            List<ProfileDtos.PracticeLocationDto> in) {
        if (in == null) {
            return List.of();
        }
        return in.stream()
                .map(l -> new CreateDoctorProfileUseCase.PracticeLocationDto(
                        l.name(), l.address(), l.postalCode(), l.city(), l.phone()))
                .toList();
    }
}
