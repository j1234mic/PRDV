package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.PractitionerDtos;
import com.prdv.rdv.profile.application.port.input.PracticeLocationUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Cabinets et lieux d'exercice (module 2.2) : multi-lieux, adresse et
 * geolocalisation, photos, visite virtuelle 360 degres, horaires, telephones,
 * fax, email professionnel, site web, reseaux sociaux, accessibilite PMR,
 * parking et transports en commun.
 */
@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Cabinets", description = "Lieux d'exercice, horaires, accessibilite, photos")
public class PracticeLocationController {

    private final PracticeLocationUseCase locationUseCase;

    public PracticeLocationController(PracticeLocationUseCase locationUseCase) {
        this.locationUseCase = locationUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('profile.location.read')")
    @Operation(summary = "Mes lieux d'exercice (lieu principal en premier)")
    public List<ProfileViews.PracticeLocationView> myLocations() {
        return locationUseCase.myLocations();
    }

    @GetMapping("/practitioners/{practitionerUserId}")
    @PreAuthorize("hasAuthority('profile.location.read')")
    public List<ProfileViews.PracticeLocationView> locationsOf(@PathVariable Long practitionerUserId) {
        return locationUseCase.locationsOf(practitionerUserId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('profile.location.write')")
    @Operation(summary = "Cree un lieu ; le premier cree devient le lieu principal")
    public ProfileViews.PracticeLocationView create(
            @Valid @RequestBody PractitionerDtos.LocationRequest request,
            @RequestParam(defaultValue = "false") boolean mainLocation) {
        return locationUseCase.create(request.toCreateCommand(mainLocation));
    }

    @PutMapping("/{locationId}")
    @PreAuthorize("hasAuthority('profile.location.write')")
    public ProfileViews.PracticeLocationView update(
            @PathVariable String locationId,
            @Valid @RequestBody PractitionerDtos.LocationRequest request) {
        return locationUseCase.update(locationId, request.toUpdateCommand());
    }

    @DeleteMapping("/{locationId}")
    @PreAuthorize("hasAuthority('profile.location.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String locationId) {
        locationUseCase.delete(locationId);
    }

    @PostMapping(value = "/{locationId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.location.write')")
    public ProfileViews.PracticeLocationView addPhoto(
            @PathVariable String locationId,
            @RequestPart("photo") @Valid PractitionerDtos.LocationPhotoRequest request,
            @RequestPart("file") MultipartFile file) {
        return locationUseCase.addPhoto(request.toCommand(locationId, file));
    }

    @DeleteMapping("/{locationId}/photos/{photoId}")
    @PreAuthorize("hasAuthority('profile.location.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime une photo du cabinet (photoId figure dans la fiche du lieu)")
    public void removePhoto(@PathVariable String locationId, @PathVariable String photoId) {
        locationUseCase.removePhoto(locationId, photoId);
    }

    @PostMapping("/{locationId}/main")
    @PreAuthorize("hasAuthority('profile.location.write')")
    @Operation(summary = "Designe ce lieu comme lieu principal (l'ancien perd ce statut)")
    public ProfileViews.PracticeLocationView promoteToMain(@PathVariable String locationId) {
        return locationUseCase.promoteToMain(locationId);
    }
}
