package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.PatientIdentityDtos;
import com.prdv.rdv.profile.application.port.input.PatientIdentityUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Profil patient complet (module 2.1) : etat civil, contacts multiples,
 * adresses, personne a prevenir, medecin traitant declare, numero de securite
 * sociale, mutuelles, photo, piece d'identite, carte Vitale (NFC / scan) et
 * partage avec le DMP national.
 */
@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profil patient", description = "Identite, contacts, mutuelles, carte Vitale, DMP")
public class PatientIdentityController {

    private final PatientIdentityUseCase identityUseCase;

    public PatientIdentityController(PatientIdentityUseCase identityUseCase) {
        this.identityUseCase = identityUseCase;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('profile.identity.read')")
    @Operation(summary = "Profil patient de l'utilisateur connecte")
    public ProfileViews.PatientIdentityView myIdentity() {
        return identityUseCase.myIdentity();
    }

    @GetMapping("/{patientUserId}")
    @PreAuthorize("hasAuthority('profile.identity.read')")
    @Operation(summary = "Profil d'un patient (soumis aux regles de visibilite)")
    public ProfileViews.PatientIdentityView identityOf(@PathVariable Long patientUserId) {
        return identityUseCase.identityOf(patientUserId);
    }

    @PutMapping("/civil-status")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    public ProfileViews.PatientIdentityView updateCivilStatus(
            @Valid @RequestBody PatientIdentityDtos.CivilStatusRequest request) {
        return identityUseCase.updateCivilStatus(request.toCommand());
    }

    @PutMapping("/contacts")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    @Operation(summary = "Remplace la liste des coordonnees (fixe, mobile, pro, fax, email)")
    public ProfileViews.PatientIdentityView replaceContacts(
            @Valid @RequestBody PatientIdentityDtos.ContactsRequest request) {
        return identityUseCase.replaceContacts(request.toCommand());
    }

    @PutMapping("/addresses")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    @Operation(summary = "Remplace la liste des adresses (domicile, travail, autre)")
    public ProfileViews.PatientIdentityView replaceAddresses(
            @Valid @RequestBody PatientIdentityDtos.AddressesRequest request) {
        return identityUseCase.replaceAddresses(request.toCommand());
    }

    @PutMapping("/emergency-contact")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    public ProfileViews.PatientIdentityView updateEmergencyContact(
            @Valid @RequestBody PatientIdentityDtos.EmergencyContactRequest request) {
        return identityUseCase.updateEmergencyContact(request.toCommand());
    }

    @PutMapping("/treating-physician")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    public ProfileViews.PatientIdentityView declareTreatingPhysician(
            @Valid @RequestBody PatientIdentityDtos.TreatingPhysicianRequest request) {
        return identityUseCase.declareTreatingPhysician(request.toCommand());
    }

    @PostMapping("/social-security-number")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    @Operation(summary = "Enregistre le numero de securite sociale (tokenise et masque, jamais stocke en clair)")
    public ProfileViews.PatientIdentityView registerSocialSecurityNumber(
            @Valid @RequestBody PatientIdentityDtos.SocialSecurityNumberRequest request) {
        return identityUseCase.registerSocialSecurityNumber(request.toCommand());
    }

    @PutMapping("/insurances")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    @Operation(summary = "Mutuelle principale ou complementaire")
    public ProfileViews.PatientIdentityView updateInsurance(
            @Valid @RequestBody PatientIdentityDtos.InsuranceRequest request) {
        return identityUseCase.updateInsurance(request.toCommand());
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.identity.update')")
    public ProfileViews.PatientIdentityView uploadProfilePhoto(@RequestPart("file") MultipartFile file) {
        return identityUseCase.uploadProfilePhoto(PatientIdentityDtos.toStoreFile(file));
    }

    @PostMapping(value = "/identity-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.identity.update')")
    @Operation(summary = "Piece d'identite : stockage chiffre, acces trace dans l'audit")
    public ProfileViews.PatientIdentityView uploadIdentityDocument(
            @RequestPart("file") MultipartFile file) {
        return identityUseCase.uploadIdentityDocument(PatientIdentityDtos.toStoreFile(file));
    }

    @PostMapping(value = "/vitale-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('profile.identity.update')")
    @Operation(summary = "Carte Vitale : lecture NFC, scan ou saisie manuelle (scan optionnel)")
    public ProfileViews.PatientIdentityView registerVitaleCard(
            @RequestPart("card") @Valid PatientIdentityDtos.VitaleCardRequest card,
            @RequestPart(value = "scan", required = false) MultipartFile scan) {
        return identityUseCase.registerVitaleCard(card.toCommand(scan));
    }

    @PostMapping("/dmp/link")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    public ProfileViews.PatientIdentityView linkDmp(
            @Valid @RequestBody PatientIdentityDtos.DmpLinkRequest request) {
        return identityUseCase.linkDmp(request.toCommand());
    }

    @PutMapping("/dmp/sharing")
    @PreAuthorize("hasAuthority('profile.privacy.update')")
    @Operation(summary = "Active ou coupe le partage avec le DMP national (consentement explicite)")
    public ProfileViews.PatientIdentityView updateDmpSharing(
            @Valid @RequestBody PatientIdentityDtos.DmpSharingRequest request) {
        return identityUseCase.updateDmpSharing(request.toCommand());
    }

    @PostMapping("/dmp/sync")
    @PreAuthorize("hasAuthority('profile.identity.update')")
    @Operation(summary = "Rapatrie depuis le DMP national les elements disponibles")
    public ProfileViews.DmpAccountView synchronizeDmp() {
        return identityUseCase.synchronizeDmp();
    }
}
