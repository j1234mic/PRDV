package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;

import java.util.Optional;

/** Persistance des preferences et des preuves de consentement. */
public interface PrivacyPreferencesRepository {

    PrivacyPreferences save(PrivacyPreferences preferences);

    Optional<PrivacyPreferences> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
