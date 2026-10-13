package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.SecurityCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Securite avancee du Module 1 :
 * <ul>
 *   <li>Chiffrement end-to-end (E2EE) des donnees medicales (annuaire de cles
 *       publiques + scellement d'enveloppes chiffrees)</li>
 *   <li>Supervision du WAF applicatif et de la protection anti-DDoS</li>
 *   <li>Campagnes de tests d'intrusion (Penetration Testing regulier) et
 *       auto-evaluation de posture de securite</li>
 *   <li>Programme de Bug Bounty et divulgation responsable (RFC 9116 security.txt)</li>
 * </ul>
 */
public interface AdvancedSecurityUseCase {

    Views.E2eePublicKeyView registerE2eeKey(SecurityCommands.RegisterE2eeKey command);

    Views.E2eePublicKeyView getActiveE2eeKey(Long userId);

    Views.E2eeEnvelopeView sealMedicalData(SecurityCommands.SealMedicalPayload command);

    String openMedicalData(SecurityCommands.OpenMedicalPayload command);

    Views.PentestReportView runAutomatedSecuritySelfCheck();

    Views.PentestReportView recordPentestReport(SecurityCommands.RecordPentestReport command);

    List<Views.PentestReportView> listPentestReports();

    Views.BugBountyPolicyView bugBountyPolicy();

    String securityTxtDescriptor();

    Views.BugBountyReportView submitBugBountyReport(SecurityCommands.SubmitBugBountyReport command);

    List<Views.BugBountyReportView> listBugBountyReports();

    Views.BugBountyReportView triageBugBountyReport(SecurityCommands.TriageBugBountyReport command);

    Views.WafDdosStatusView wafDdosStatus();

    void unblockIpAddress(String ipAddress);
}
