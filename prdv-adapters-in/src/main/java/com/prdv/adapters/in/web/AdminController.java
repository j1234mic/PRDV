package com.prdv.adapters.in.web;

import com.prdv.adapters.in.web.dto.ProfileDtos.DoctorProfileResponse;
import com.prdv.adapters.in.web.dto.ProfileDtos.RejectRequest;
import com.prdv.profile.application.port.in.ValidateDoctorProfileUseCase;
import com.prdv.profile.application.port.out.DoctorProfileRepository;
import com.prdv.shared.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Moderation (acces ADMIN deja garanti par la config HTTP - defense en profondeur possible ici aussi). */
@RestController
@RequestMapping("/api/admin/doctors")
public class AdminController {

    private final DoctorProfileRepository doctors;
    private final ValidateDoctorProfileUseCase validation;

    public AdminController(DoctorProfileRepository doctors, ValidateDoctorProfileUseCase validation) {
        this.doctors = doctors;
        this.validation = validation;
    }

    @GetMapping
    public List<DoctorProfileResponse> queue(@RequestParam(defaultValue = "PENDING_VALIDATION") String status) {
        return doctors.findAllByVerificationStatus(status, 50).stream().map(DtoMapper::toResponse).toList();
    }

    @PostMapping("/{userId}/approve")
    public void approve(@PathVariable Long userId) {
        validation.approve(userId);
    }

    @PostMapping("/{userId}/reject")
    public void reject(@PathVariable Long userId, @RequestBody(required = false) RejectRequest request) {
        validation.reject(userId, request == null || request.reason() == null ? "Motif non precise" : request.reason());
    }

    @GetMapping("/{userId}")
    public DoctorProfileResponse detail(@PathVariable Long userId) {
        return DtoMapper.toResponse(doctors.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profil introuvable")));
    }
}
