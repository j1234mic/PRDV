package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.adapter.out.persistence.converter.JsonConverters;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistance des preferences et de l'historique complet des consentements
 * RGPD (la preuve de consentement doit etre conservée meme apres retrait).
 */
@Entity
@Table(name = "profile_privacy_preferences", indexes = {
        @Index(name = "idx_privacy_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
public class PrivacyPreferencesEntity extends ProfileEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Lob
    @Convert(converter = JsonConverters.StringList.class)
    @Column(name = "preferred_languages")
    private List<String> preferredLanguages = new ArrayList<>(List.of(PrivacyPreferences.DEFAULT_LANGUAGE));

    @Column(length = 10)
    private String primaryLanguage = PrivacyPreferences.DEFAULT_LANGUAGE;

    // --- Accessibilite ------------------------------------------------------
    @Lob
    @Convert(converter = JsonConverters.AccessibilityNeedSet.class)
    @Column(name = "accessibility_needs")
    private Set<PrivacyPreferences.AccessibilityNeed> accessibilityNeeds = new LinkedHashSet<>();

    private boolean screenReaderOptimized;

    private boolean largePrint;

    @Column(length = 10)
    private String signLanguage;

    private boolean subtitlesRequired;

    // --- Communication --------------------------------------------------------
    @Lob
    @Convert(converter = JsonConverters.CommunicationChannelSet.class)
    @Column(name = "preferred_channels")
    private Set<PrivacyPreferences.CommunicationChannel> preferredChannels = new LinkedHashSet<>();

    private boolean quietHoursEnabled;

    private LocalTime quietHoursStart;

    private LocalTime quietHoursEnd;

    // --- Consentements et granularite -------------------------------------------
    @Lob
    @Convert(converter = JsonConverters.ConsentRecordList.class)
    @Column(name = "consent_history")
    private List<PrivacyPreferences.ConsentRecord> consentHistory = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.VisibilityRuleList.class)
    @Column(name = "visibility_rules")
    private List<PrivacyPreferences.VisibilityRule> visibilityRules = new ArrayList<>();

    private boolean dmpSharingEnabled;

    private Instant erasureRequestedAt;
}
