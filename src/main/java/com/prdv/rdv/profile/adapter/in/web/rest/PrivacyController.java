package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.PrivacyDtos;
import com.prdv.rdv.profile.application.port.input.DataPortabilityUseCase;
import com.prdv.rdv.profile.application.port.input.PrivacyPreferencesUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Preferences, confidentialite et portabilite (module 2.1) : langues,
 * accessibilite (malvoyant, sourd), preferences de communication, consentements
 * RGPD avec preuve, granularite « qui peut voir quoi », export des donnees
 * (art. 20) et suppression de compte (art. 17).
 */
@RestController
@RequestMapping("/api/v1/privacy")
@Tag(name = "Confidentialite", description = "Consentements RGPD, visibilite, export, effacement")
public class PrivacyController {

    private final PrivacyPreferencesUseCase preferencesUseCase;
    private final DataPortabilityUseCase portabilityUseCase;

    public PrivacyController(PrivacyPreferencesUseCase preferencesUseCase,
                             DataPortabilityUseCase portabilityUseCase) {
        this.preferencesUseCase = preferencesUseCase;
        this.portabilityUseCase = portabilityUseCase;
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasAuthority('profile.privacy.read')")
    public ProfileViews.PrivacyPreferencesView myPreferences() {
        return preferencesUseCase.myPreferences();
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasAuthority('profile.privacy.update')")
    @Operation(summary = "Langues, accessibilite et preferences de communication")
    public ProfileViews.PrivacyPreferencesView update(
            @Valid @RequestBody PrivacyDtos.PreferencesRequest request) {
        return preferencesUseCase.update(request.toCommand());
    }

    @PostMapping("/consents")
    @PreAuthorize("hasAuthority('profile.privacy.update')")
    @Operation(summary = "Enregistre un consentement avec sa preuve (version de politique, IP, horodatage)")
    public ProfileViews.PrivacyPreferencesView recordConsent(
            @Valid @RequestBody PrivacyDtos.ConsentRequest request, HttpServletRequest httpRequest) {
        return preferencesUseCase.recordConsent(request.toCommand(clientIp(httpRequest)));
    }

    @DeleteMapping("/consents/{purpose}")
    @PreAuthorize("hasAuthority('profile.privacy.update')")
    @Operation(summary = "Retrait de consentement : l'historique est conserve, le retrait est date")
    public ProfileViews.PrivacyPreferencesView withdrawConsent(
            @PathVariable PrivacyPreferences.ConsentPurpose purpose,
            HttpServletRequest httpRequest) {
        return preferencesUseCase.withdrawConsent(
                new PrivacyDtos.WithdrawRequest(purpose).toCommand(clientIp(httpRequest)));
    }

    @PutMapping("/visibility")
    @PreAuthorize("hasAuthority('profile.privacy.update')")
    @Operation(summary = "Granularite : qui peut voir quelle categorie de donnees")
    public ProfileViews.PrivacyPreferencesView setVisibility(
            @Valid @RequestBody PrivacyDtos.VisibilityRequest request) {
        return preferencesUseCase.setVisibility(request.toCommand());
    }

    @PutMapping("/dmp-sharing")
    @PreAuthorize("hasAuthority('profile.privacy.update')")
    @Operation(summary = "Partage avec le DMP national : consentement explicite et revoquable")
    public ProfileViews.PrivacyPreferencesView updateDmpSharing(
            @Valid @RequestBody PrivacyDtos.DmpSharingRequest request, HttpServletRequest httpRequest) {
        return preferencesUseCase.updateDmpSharing(request.toCommand(clientIp(httpRequest)));
    }

    @PostMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAuthority('profile.data.export')")
    @Operation(summary = "Export RGPD (art. 20) : profil, dossier medical, documents, mesures, consentements")
    public ResponseEntity<byte[]> exportMyData(@Valid @RequestBody PrivacyDtos.ExportRequest request) {
        ProfileViews.DataExportView export = portabilityUseCase.exportMyData(request.toFormat());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(export.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(export.sizeBytes())
                .body(export.content().getBytes(StandardCharsets.UTF_8));
    }

    @DeleteMapping("/account")
    @PreAuthorize("hasAuthority('profile.data.erase')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Effacement RGPD (art. 17) : donnees purgees, compte anonymise")
    public void requestErasure(@RequestBody(required = false) PrivacyDtos.ErasureRequest request,
                               HttpServletRequest httpRequest) {
        PrivacyDtos.ErasureRequest effective = request == null ? new PrivacyDtos.ErasureRequest(null)
                : request;
        portabilityUseCase.requestErasure(effective.toCommand(clientIp(httpRequest)));
    }

    /** IP du demandeur : element de preuve des consentements. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
