package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.SecurityDtos;
import com.prdv.rdv.iam.application.command.SecurityCommands;
import com.prdv.rdv.iam.application.port.input.AdvancedSecurityUseCase;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de securite avancee (Module 1.2) :
 * <ul>
 *   <li>Chiffrement end-to-end (E2EE) des donnees medicales</li>
 *   <li>Supervision du WAF applicatif et protection anti-DDoS</li>
 *   <li>Penetration Testing regulier (auto-diagnostic + rapports d'audit)</li>
 *   <li>Bug Bounty Program et descripteur RFC 9116 ({@code /.well-known/security.txt})</li>
 * </ul>
 */
@RestController
@Tag(name = "Securite Avancee", description = "Chiffrement E2EE medical, WAF/DDoS, Penetration Testing, Bug Bounty")
public class SecurityController {

    private final AdvancedSecurityUseCase advancedSecurity;

    public SecurityController(AdvancedSecurityUseCase advancedSecurity) {
        this.advancedSecurity = advancedSecurity;
    }

    // ------------------------------------------------- Chiffrement E2EE medical
    @PostMapping("/api/v1/security/e2ee/keys")
    @PreAuthorize("hasAuthority('iam.security.e2ee')")
    @Operation(summary = "Enregistrer sa cle publique de chiffrement de bout en bout (E2EE)")
    public ResponseEntity<Views.E2eePublicKeyView> registerE2eeKey(
            @Valid @RequestBody SecurityDtos.RegisterE2eeKeyRequest body) {
        Views.E2eePublicKeyView view = advancedSecurity.registerE2eeKey(
                new SecurityCommands.RegisterE2eeKey(
                        body.keyId(), body.algorithm(), body.publicKeyPem()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/api/v1/security/e2ee/keys/{userId}")
    @PreAuthorize("hasAuthority('iam.security.e2ee')")
    @Operation(summary = "Recuperer la cle publique E2EE active d'un destinataire")
    public ResponseEntity<Views.E2eePublicKeyView> getActiveE2eeKey(@PathVariable Long userId) {
        return ResponseEntity.ok(advancedSecurity.getActiveE2eeKey(userId));
    }

    @PostMapping("/api/v1/security/e2ee/seal")
    @PreAuthorize("hasAuthority('iam.security.e2ee')")
    @Operation(summary = "Sceller (chiffrer de bout en bout) une donnee medicale pour un destinataire")
    public ResponseEntity<Views.E2eeEnvelopeView> sealMedicalData(
            @Valid @RequestBody SecurityDtos.SealMedicalPayloadRequest body) {
        return ResponseEntity.ok(advancedSecurity.sealMedicalData(
                new SecurityCommands.SealMedicalPayload(body.recipientUserId(), body.plainPayload())));
    }

    @PostMapping("/api/v1/security/e2ee/open")
    @PreAuthorize("hasAuthority('iam.security.e2ee')")
    @Operation(summary = "Ouvrir (dechiffrer) une enveloppe medicale E2EE adressee a l'utilisateur connecte")
    public ResponseEntity<SecurityDtos.DecryptedMedicalPayloadResponse> openMedicalData(
            @Valid @RequestBody SecurityDtos.OpenMedicalPayloadRequest body) {
        String plain = advancedSecurity.openMedicalData(
                new SecurityCommands.OpenMedicalPayload(body.cipherText()));
        return ResponseEntity.ok(new SecurityDtos.DecryptedMedicalPayloadResponse(plain));
    }

    // ------------------------------------------------- WAF & Protection DDoS
    @GetMapping("/api/v1/security/waf/status")
    @PreAuthorize("hasAuthority('iam.security.admin')")
    @Operation(summary = "Etat temps-reel du WAF applicatif et de la protection DDoS")
    public ResponseEntity<Views.WafDdosStatusView> wafDdosStatus() {
        return ResponseEntity.ok(advancedSecurity.wafDdosStatus());
    }

    @DeleteMapping("/api/v1/security/waf/blocked-ips/{ipAddress}")
    @PreAuthorize("hasAuthority('iam.security.admin')")
    @Operation(summary = "Debloquer une adresse IP bannie par la protection WAF/DDoS")
    public ResponseEntity<Void> unblockIp(@PathVariable String ipAddress) {
        advancedSecurity.unblockIpAddress(ipAddress);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------- Penetration Testing
    @PostMapping("/api/v1/security/pentest/self-check")
    @PreAuthorize("hasAuthority('iam.security.admin')")
    @Operation(summary = "Executer un auto-audit de securite et generer un rapport de Penetration Testing")
    public ResponseEntity<Views.PentestReportView> runAutomatedPentest() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(advancedSecurity.runAutomatedSecuritySelfCheck());
    }

    @PostMapping("/api/v1/security/pentest/reports")
    @PreAuthorize("hasAuthority('iam.security.admin')")
    @Operation(summary = "Enregistrer un rapport de campagne de Penetration Testing")
    public ResponseEntity<Views.PentestReportView> recordPentestReport(
            @Valid @RequestBody SecurityDtos.RecordPentestReportRequest body) {
        Views.PentestReportView view = advancedSecurity.recordPentestReport(
                new SecurityCommands.RecordPentestReport(
                        body.campaignName(),
                        body.auditor(),
                        body.scope(),
                        body.criticalCount(),
                        body.highCount(),
                        body.mediumCount(),
                        body.lowCount(),
                        body.summary(),
                        body.findings(),
                        body.nextScheduledAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/api/v1/security/pentest/reports")
    @PreAuthorize("hasAuthority('iam.security.admin')")
    @Operation(summary = "Lister les rapports de Penetration Testing")
    public List<Views.PentestReportView> listPentestReports() {
        return advancedSecurity.listPentestReports();
    }

    // ------------------------------------------------- Bug Bounty Program
    @GetMapping(value = "/.well-known/security.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Descripteur normalise RFC 9116 pour la divulgation de vulnerabilites")
    public ResponseEntity<String> securityTxt() {
        return ResponseEntity.ok(advancedSecurity.securityTxtDescriptor());
    }

    @GetMapping("/api/v1/security/bug-bounty/policy")
    @Operation(summary = "Politique publique et grille de primes du programme Bug Bounty")
    public ResponseEntity<Views.BugBountyPolicyView> bugBountyPolicy() {
        return ResponseEntity.ok(advancedSecurity.bugBountyPolicy());
    }

    @PostMapping("/api/v1/security/bug-bounty/reports")
    @Operation(summary = "Soumettre un rapport de vulnerabilite au programme Bug Bounty")
    public ResponseEntity<Views.BugBountyReportView> submitBugBountyReport(
            @Valid @RequestBody SecurityDtos.SubmitBugBountyReportRequest body) {
        Views.BugBountyReportView view = advancedSecurity.submitBugBountyReport(
                new SecurityCommands.SubmitBugBountyReport(
                        body.reporterEmail(),
                        body.reporterAlias(),
                        body.title(),
                        body.description(),
                        body.affectedComponent(),
                        body.severity(),
                        body.cvssScore()));
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/api/v1/security/bug-bounty/reports")
    @PreAuthorize("hasAuthority('iam.security.admin')")
    @Operation(summary = "Lister les rapports de vulnerabilites Bug Bounty soumis")
    public List<Views.BugBountyReportView> listBugBountyReports() {
        return advancedSecurity.listBugBountyReports();
    }

    @PostMapping("/api/v1/security/bug-bounty/reports/{id}/triage")
    @PreAuthorize("hasAuthority('iam.security.admin')")
    @Operation(summary = "Qualifier / attribuer une prime a un rapport Bug Bounty")
    public ResponseEntity<Views.BugBountyReportView> triageBugBountyReport(
            @PathVariable Long id,
            @Valid @RequestBody SecurityDtos.TriageBugBountyReportRequest body) {
        return ResponseEntity.ok(advancedSecurity.triageBugBountyReport(
                new SecurityCommands.TriageBugBountyReport(
                        id, body.status(), body.bountyAmountCents(), body.resolutionNote())));
    }
}
