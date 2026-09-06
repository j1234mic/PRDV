package com.prdv.adapters.in.web;

import com.prdv.adapters.in.web.dto.ProfileDtos.DoctorProfileResponse;
import com.prdv.profile.application.port.in.DoctorDirectoryUseCase;
import com.prdv.profile.application.port.out.DoctorProfileRepository;
import com.prdv.shared.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Annuaire public : seuls les praticiens VERIFIES sont exposables. */
@RestController
@RequestMapping("/api/doctors")
public class DirectoryController {

    private final DoctorDirectoryUseCase directory;
    private final DoctorProfileRepository doctors;

    public DirectoryController(DoctorDirectoryUseCase directory, DoctorProfileRepository doctors) {
        this.directory = directory;
        this.doctors = doctors;
    }

    @GetMapping
    public Object search(@RequestParam(required = false) String specialty,
                         @RequestParam(defaultValue = "20") int limit) {
        return directory.search(specialty, limit);
    }

    @GetMapping("/{userId}/profile")
    public DoctorProfileResponse profile(@PathVariable Long userId) {
        var profile = doctors.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Praticien introuvable"));
        if (profile.verification() != com.prdv.profile.domain.model.DoctorVerificationStatus.VERIFIED) {
            throw new NotFoundException("Praticien introuvable");
        }
        return DtoMapper.toResponse(profile);
    }
}
