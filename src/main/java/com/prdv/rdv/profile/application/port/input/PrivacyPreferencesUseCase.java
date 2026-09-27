package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.PrivacyCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

/** Cas d'usage : langues, accessibilite, communication, consentements et granularite. */
public interface PrivacyPreferencesUseCase {

    ProfileViews.PrivacyPreferencesView myPreferences();

    ProfileViews.PrivacyPreferencesView update(PrivacyCommands.UpdatePreferences command);

    ProfileViews.PrivacyPreferencesView recordConsent(PrivacyCommands.RecordConsent command);

    ProfileViews.PrivacyPreferencesView withdrawConsent(PrivacyCommands.WithdrawConsent command);

    ProfileViews.PrivacyPreferencesView setVisibility(PrivacyCommands.SetVisibility command);

    ProfileViews.PrivacyPreferencesView updateDmpSharing(PrivacyCommands.UpdateDmpSharing command);
}
