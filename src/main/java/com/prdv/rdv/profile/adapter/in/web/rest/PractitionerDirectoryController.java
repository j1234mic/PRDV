package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.PractitionerDtos;
import com.prdv.rdv.profile.application.port.input.PractitionerDirectoryUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.practitioner.Badge;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Annuaire des praticiens (module 2.2) : recherche publique par nom,
 * specialite, ville, teleconsultation et accessibilite PMR, fiche publique
 * filtree selon les regles de visibilite, avis patients et notation.
 *
 * <p>La lecture est publique (permitAll dans la configuration de securite) ;
 * la notation exige un compte patient authentifie.
 */
@RestController
@RequestMapping("/api/v1/directory")
@Tag(name = "Annuaire praticiens", description = "Recherche publique, fiche, avis et notes")
public class PractitionerDirectoryController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PractitionerDirectoryUseCase directoryUseCase;

    public PractitionerDirectoryController(PractitionerDirectoryUseCase directoryUseCase) {
        this.directoryUseCase = directoryUseCase;
    }

    @GetMapping("/practitioners")
    @Operation(summary = "Recherche : terme, specialite, ville, teleconsultation, accessibilite PMR, badge")
    public ProfileViews.PagedResult<ProfileViews.DirectoryEntryView> search(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "false") boolean teleconsultationOnly,
            @RequestParam(defaultValue = "false") boolean wheelchairAccessibleOnly,
            @RequestParam(required = false) Badge.BadgeType badge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        return directoryUseCase.search(term, specialty, city, teleconsultationOnly,
                wheelchairAccessibleOnly, badge, page, size);
    }

    @GetMapping("/practitioners/{practitionerUserId}")
    @Operation(summary = "Fiche publique (donnees non publiables exclues)")
    public ProfileViews.PractitionerDossierView publicProfile(@PathVariable Long practitionerUserId) {
        return directoryUseCase.publicProfile(practitionerUserId);
    }

    @GetMapping("/practitioners/{practitionerUserId}/photo")
    @Operation(summary = "Photo professionnelle (fiche publiable uniquement)")
    public ResponseEntity<byte[]> practitionerPhoto(@PathVariable Long practitionerUserId) {
        return FileResponses.of(directoryUseCase.practitionerPhoto(practitionerUserId), false);
    }

    @GetMapping("/practitioners/{practitionerUserId}/presentation-video")
    @Operation(summary = "Video de presentation (fiche publiable uniquement)")
    public ResponseEntity<byte[]> presentationVideo(@PathVariable Long practitionerUserId) {
        return FileResponses.of(directoryUseCase.presentationVideo(practitionerUserId), false);
    }

    @GetMapping("/practitioners/{practitionerUserId}/locations/{locationId}/photos/{photoId}")
    @Operation(summary = "Photo d'un cabinet (identifiant photoId fourni par la fiche du lieu)")
    public ResponseEntity<byte[]> locationPhoto(@PathVariable Long practitionerUserId,
                                                @PathVariable String locationId,
                                                @PathVariable String photoId) {
        return FileResponses.of(directoryUseCase.locationPhoto(practitionerUserId, locationId, photoId), false);
    }

    @GetMapping("/practitioners/{practitionerUserId}/ratings")
    @Operation(summary = "Avis visibles ; l'identite du patient n'est exposee qu'au praticien concerne")
    public List<ProfileViews.PractitionerRatingView> ratingsOf(@PathVariable Long practitionerUserId) {
        return directoryUseCase.ratingsOf(practitionerUserId);
    }

    @PostMapping("/practitioners/{practitionerUserId}/ratings")
    @PreAuthorize("hasAuthority('profile.rating.write')")
    @Operation(summary = "Un patient ne peut noter un praticien qu'une seule fois")
    public ProfileViews.PractitionerRatingView submitRating(
            @PathVariable Long practitionerUserId,
            @Valid @RequestBody PractitionerDtos.RatingRequest request) {
        return directoryUseCase.submitRating(practitionerUserId, request.toCommand());
    }
}
