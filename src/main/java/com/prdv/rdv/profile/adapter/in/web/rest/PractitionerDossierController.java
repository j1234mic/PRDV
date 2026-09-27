package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.PractitionerDtos;
import com.prdv.rdv.profile.application.port.input.PractitionerProfileUseCase;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Profil medecin / praticien (module 2.2) : identite professionnelle (titre,
 * RPPS/ADELI, ordre, specialites, diplomes, langues, photo, video),
 * informations medicales (secteur, tarifs, OPTAM, tiers-payant, actes,
 * equipements, teleconsultation), gestion (SIRET, RIB, RC pro, URSSAF,
 * AGA/CGA, cotisation ordinale, certifications), reseau professionnel et
 * visibilite / marketing.
 *
 * <p>Les badges « verifie », « populaire » et « nouveau » sont derives par le
 * domaine, jamais saisis.
 */
@RestController
@RequestMapping("/api/v1/practitioner-dossier")
@Tag(name = "Profil praticien", description = "Identite pro, tarifs, gestion, reseau, visibilite")
public class PractitionerDossierController {

    private final PractitionerProfileUseCase dossierUseCase;

    public PractitionerDossierController(PractitionerProfileUseCase dossierUseCase) {
        this.dossierUseCase = dossierUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('profile.dossier.read')")
    public ProfileViews.PractitionerDossierView myDossier() {
        return dossierUseCase.myDossier();
    }

    @GetMapping("/{practitionerUserId}")
    @PreAuthorize("hasAuthority('profile.dossier.read')")
    public ProfileViews.PractitionerDossierView dossierOf(@PathVariable Long practitionerUserId) {
        return dossierUseCase.dossierOf(practitionerUserId);
    }

    @PutMapping("/identity")
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    @Operation(summary = "Titre, RPPS/ADELI, ordre, specialites, diplomes, langues (photo et video conservees)")
    public ProfileViews.PractitionerDossierView updateIdentity(
            @Valid @RequestBody PractitionerDtos.IdentityRequest request) {
        return dossierUseCase.updateIdentity(request.toCommand());
    }

    @PutMapping("/practice-information")
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    @Operation(summary = "Secteur de convention, tarifs, OPTAM, tiers-payant, actes, equipements, ages, teleconsultation")
    public ProfileViews.PractitionerDossierView updatePracticeInformation(
            @Valid @RequestBody PractitionerDtos.PracticeInformationRequest request) {
        return dossierUseCase.updatePracticeInformation(request.toCommand());
    }

    @PutMapping("/management")
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    @Operation(summary = "SIRET, RIB (tokenise et masque), RC pro, URSSAF, AGA/CGA, cotisation ordinale")
    public ProfileViews.PractitionerDossierView updateManagement(
            @Valid @RequestBody PractitionerDtos.ManagementRequest request) {
        return dossierUseCase.updateManagement(request.toCommand());
    }

    @PutMapping("/visibility")
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    @Operation(summary = "Description longue, domaines d'expertise, publications, distinctions, medias")
    public ProfileViews.PractitionerDossierView updateVisibility(
            @Valid @RequestBody PractitionerDtos.VisibilityRequest request) {
        return dossierUseCase.updateVisibility(request.toCommand());
    }

    @PostMapping("/network-contacts")
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    @Operation(summary = "Correspondants, specialistes, laboratoires, pharmacies, imagerie, hopitaux")
    public ProfileViews.PractitionerDossierView addNetworkContact(
            @Valid @RequestBody PractitionerDtos.NetworkContactRequest request) {
        return dossierUseCase.addNetworkContact(request.toCommand());
    }

    @DeleteMapping("/network-contacts/{contactId}")
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeNetworkContact(@PathVariable String contactId) {
        dossierUseCase.removeNetworkContact(contactId);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    public ProfileViews.PractitionerDossierView uploadPhoto(@RequestPart("file") MultipartFile file) {
        return dossierUseCase.uploadPhoto(PractitionerDtos.toStoreMedia(file, false));
    }

    @PostMapping(value = "/presentation-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    public ProfileViews.PractitionerDossierView uploadPresentationVideo(
            @RequestPart("file") MultipartFile file) {
        return dossierUseCase.uploadPresentationVideo(PractitionerDtos.toStoreMedia(file, true));
    }

    @PostMapping("/badges/refresh")
    @PreAuthorize("hasAuthority('profile.dossier.write')")
    @Operation(summary = "Recalcule les badges (verifie, populaire, nouveau) a partir des regles du domaine")
    public ProfileViews.PractitionerDossierView refreshBadges() {
        return dossierUseCase.refreshBadges();
    }

}
