package com.prdv.rdv.profile.application.command;

import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Commandes du cas d'usage « preferences &amp; confidentialite » (module 2.1).
 */
public final class PrivacyCommands {

    private PrivacyCommands() {
    }

    public record AccessibilitySettingsCommand(Set<PrivacyPreferences.AccessibilityNeed> needs,
                                               boolean screenReaderOptimized,
                                               boolean largePrint,
                                               String signLanguage,
                                               boolean subtitlesRequired) {
    }

    public record CommunicationSettingsCommand(Set<PrivacyPreferences.CommunicationChannel> preferredChannels,
                                               boolean quietHoursEnabled,
                                               LocalTime quietHoursStart,
                                               LocalTime quietHoursEnd) {
    }

    public record UpdatePreferences(List<String> preferredLanguages,
                                    String primaryLanguage,
                                    AccessibilitySettingsCommand accessibility,
                                    CommunicationSettingsCommand communication) {
    }

    public record RecordConsent(PrivacyPreferences.ConsentPurpose purpose,
                                boolean granted,
                                String policyVersion,
                                String ipAddress) {
    }

    public record WithdrawConsent(PrivacyPreferences.ConsentPurpose purpose, String ipAddress) {
    }

    public record SetVisibility(PrivacyPreferences.DataCategory category,
                                PrivacyPreferences.VisibilityLevel level,
                                Set<Long> granteeUserIds) {
    }

    public record UpdateDmpSharing(boolean enabled, String policyVersion, String ipAddress) {
    }

    public record RequestErasure(String ipAddress) {
    }
}
