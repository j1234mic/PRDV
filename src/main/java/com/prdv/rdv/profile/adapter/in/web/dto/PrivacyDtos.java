package com.prdv.rdv.profile.adapter.in.web.dto;

import com.prdv.rdv.profile.application.command.PrivacyCommands;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * DTO d'entree des preferences, consentements et de la portabilite RGPD.
 *
 * <p>L'adresse IP du demandeur fait partie de la preuve de consentement : elle
 * est ajoutee par le controleur, jamais par le client.
 */
public final class PrivacyDtos {

    private PrivacyDtos() {
    }

    public record AccessibilityRequest(Set<PrivacyPreferences.AccessibilityNeed> needs,
                                       boolean screenReaderOptimized,
                                       boolean largePrint,
                                       String signLanguage,
                                       boolean subtitlesRequired) {

        public PrivacyCommands.AccessibilitySettingsCommand toCommand() {
            return new PrivacyCommands.AccessibilitySettingsCommand(needs, screenReaderOptimized, largePrint,
                    signLanguage, subtitlesRequired);
        }
    }

    public record CommunicationRequest(Set<PrivacyPreferences.CommunicationChannel> preferredChannels,
                                       boolean quietHoursEnabled,
                                       LocalTime quietHoursStart,
                                       LocalTime quietHoursEnd) {

        public PrivacyCommands.CommunicationSettingsCommand toCommand() {
            return new PrivacyCommands.CommunicationSettingsCommand(preferredChannels, quietHoursEnabled,
                    quietHoursStart, quietHoursEnd);
        }
    }

    public record PreferencesRequest(List<String> preferredLanguages,
                                     @Pattern(regexp = "[a-z]{2}(-[A-Z]{2})?") String primaryLanguage,
                                     @Valid AccessibilityRequest accessibility,
                                     @Valid CommunicationRequest communication) {

        public PrivacyCommands.UpdatePreferences toCommand() {
            return new PrivacyCommands.UpdatePreferences(preferredLanguages, primaryLanguage,
                    accessibility == null ? null : accessibility.toCommand(),
                    communication == null ? null : communication.toCommand());
        }
    }

    public record ConsentRequest(@NotNull PrivacyPreferences.ConsentPurpose purpose,
                                 boolean granted,
                                 String policyVersion) {

        public PrivacyCommands.RecordConsent toCommand(String ipAddress) {
            return new PrivacyCommands.RecordConsent(purpose, granted, policyVersion, ipAddress);
        }
    }

    public record WithdrawRequest(@NotNull PrivacyPreferences.ConsentPurpose purpose) {

        public PrivacyCommands.WithdrawConsent toCommand(String ipAddress) {
            return new PrivacyCommands.WithdrawConsent(purpose, ipAddress);
        }
    }

    /** Granularite « qui peut voir quoi ». */
    public record VisibilityRequest(@NotNull PrivacyPreferences.DataCategory category,
                                    @NotNull PrivacyPreferences.VisibilityLevel level,
                                    Set<Long> granteeUserIds) {

        public PrivacyCommands.SetVisibility toCommand() {
            return new PrivacyCommands.SetVisibility(category, level, granteeUserIds);
        }
    }

    public record DmpSharingRequest(boolean enabled, String policyVersion) {

        public PrivacyCommands.UpdateDmpSharing toCommand(String ipAddress) {
            return new PrivacyCommands.UpdateDmpSharing(enabled, policyVersion, ipAddress);
        }
    }

    public record ErasureRequest(String reason) {

        public PrivacyCommands.RequestErasure toCommand(String ipAddress) {
            return new PrivacyCommands.RequestErasure(ipAddress);
        }
    }

    /** Format d'export (RGPD art. 20). Le service valide la valeur. */
    public record ExportRequest(@NotBlank String format) {

        public String toFormat() {
            return format;
        }
    }
}
