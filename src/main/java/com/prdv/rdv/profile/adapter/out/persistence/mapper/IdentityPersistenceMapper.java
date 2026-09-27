package com.prdv.rdv.profile.adapter.out.persistence.mapper;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PatientIdentityEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.PrivacyPreferencesEntity;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Mapper domaine &lt;-&gt; entites JPA pour l'identite patient et les
 * preferences de confidentialite (le domaine ignore toute annotation JPA).
 */
@Component
public class IdentityPersistenceMapper {

    // ------------------------------------------------------------------
    // PatientIdentity
    // ------------------------------------------------------------------

    public PatientIdentityEntity toEntity(PatientIdentity identity) {
        PatientIdentityEntity entity = new PatientIdentityEntity();
        entity.setId(identity.getId());
        entity.setUserId(identity.getUserId());

        PatientIdentity.CivilStatus civilStatus = identity.getCivilStatus();
        if (civilStatus != null) {
            entity.setCivility(civilStatus.civility());
            entity.setFirstName(civilStatus.firstName());
            entity.setBirthName(civilStatus.birthName());
            entity.setLastName(civilStatus.lastName());
            entity.setPreferredName(civilStatus.preferredName());
            entity.setBirthDate(civilStatus.birthDate());
            entity.setBirthPlace(civilStatus.birthPlace());
            entity.setBirthCountry(civilStatus.birthCountry());
            entity.setGender(civilStatus.gender());
            entity.setMaritalStatus(civilStatus.maritalStatus());
            entity.setNationality(civilStatus.nationality());
        }

        entity.setContacts(new ArrayList<>(identity.getContacts()));
        entity.setAddresses(new ArrayList<>(identity.getAddresses()));

        PatientIdentity.EmergencyContact emergency = identity.getEmergencyContact();
        if (emergency != null) {
            entity.setEmergencyFirstName(emergency.firstName());
            entity.setEmergencyLastName(emergency.lastName());
            entity.setEmergencyRelationship(emergency.relationship());
            entity.setEmergencyPhone(emergency.phone());
            entity.setEmergencyEmail(emergency.email());
        }

        PatientIdentity.TreatingPhysician physician = identity.getTreatingPhysician();
        if (physician != null) {
            entity.setPhysicianFirstName(physician.firstName());
            entity.setPhysicianLastName(physician.lastName());
            entity.setPhysicianRppsNumber(physician.rppsNumber());
            entity.setPhysicianPhone(physician.phone());
            entity.setPhysicianEmail(physician.email());
            entity.setPhysicianDeclaredToInsurance(physician.declaredToInsurance());
        }

        entity.setSocialSecurityNumberToken(identity.getSocialSecurityNumberToken());
        entity.setMaskedSocialSecurityNumber(identity.getMaskedSocialSecurityNumber());
        entity.setPhotoStorageKey(identity.getPhotoStorageKey());
        entity.setIdentityDocumentStorageKey(identity.getIdentityDocumentStorageKey());
        entity.setIdentityDocumentToken(identity.getIdentityDocumentToken());

        PatientIdentity.VitaleCard vitale = identity.getVitaleCard();
        if (vitale != null) {
            entity.setVitaleNirToken(vitale.nir());
            entity.setVitaleCardVersion(vitale.cardVersion());
            entity.setVitaleIssuedOn(vitale.issuedOn());
            entity.setVitaleExpiresOn(vitale.expiresOn());
            entity.setVitaleReadMode(vitale.readMode());
            entity.setVitaleScanStorageKey(vitale.scanStorageKey());
            entity.setVitaleReadAt(vitale.readAt());
        }

        List<PatientIdentity.HealthInsurance> insurances = new ArrayList<>();
        if (identity.getPrimaryInsurance() != null) {
            insurances.add(identity.getPrimaryInsurance());
        }
        if (identity.getComplementaryInsurance() != null) {
            insurances.add(identity.getComplementaryInsurance());
        }
        entity.setInsurances(insurances);

        PatientIdentity.DmpAccount dmp = identity.getDmpAccount();
        if (dmp != null) {
            entity.setDmpIdentifier(dmp.dmpIdentifier());
            entity.setDmpLinked(dmp.linked());
            entity.setDmpSharingEnabled(dmp.sharingEnabled());
            entity.setDmpLinkedAt(dmp.linkedAt());
            entity.setDmpLastSyncAt(dmp.lastSyncAt());
        }

        entity.setCreatedAt(identity.getCreatedAt());
        entity.setUpdatedAt(identity.getUpdatedAt());
        return entity;
    }

    public PatientIdentity toDomain(PatientIdentityEntity entity) {
        PatientIdentity identity = new PatientIdentity();
        identity.setId(entity.getId());
        identity.setUserId(entity.getUserId());
        identity.setCivilStatus(new PatientIdentity.CivilStatus(entity.getCivility(),
                entity.getFirstName(), entity.getBirthName(), entity.getLastName(),
                entity.getPreferredName(), entity.getBirthDate(), entity.getBirthPlace(),
                entity.getBirthCountry(), entity.getGender(), entity.getMaritalStatus(),
                entity.getNationality()));

        identity.setContacts(entity.getContacts() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(entity.getContacts()));
        identity.setAddresses(entity.getAddresses() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(entity.getAddresses()));

        if (entity.getEmergencyLastName() != null) {
            identity.setEmergencyContact(new PatientIdentity.EmergencyContact(
                    entity.getEmergencyFirstName(), entity.getEmergencyLastName(),
                    entity.getEmergencyRelationship(), entity.getEmergencyPhone(),
                    entity.getEmergencyEmail()));
        }
        if (entity.getPhysicianLastName() != null) {
            identity.setTreatingPhysician(new PatientIdentity.TreatingPhysician(
                    entity.getPhysicianFirstName(), entity.getPhysicianLastName(),
                    entity.getPhysicianRppsNumber(), entity.getPhysicianPhone(),
                    entity.getPhysicianEmail(), entity.isPhysicianDeclaredToInsurance()));
        }

        identity.setSocialSecurityNumberToken(entity.getSocialSecurityNumberToken());
        identity.setMaskedSocialSecurityNumber(entity.getMaskedSocialSecurityNumber());
        identity.setPhotoStorageKey(entity.getPhotoStorageKey());
        identity.setIdentityDocumentStorageKey(entity.getIdentityDocumentStorageKey());
        identity.setIdentityDocumentToken(entity.getIdentityDocumentToken());

        if (entity.getVitaleReadMode() != null || entity.getVitaleNirToken() != null
                || entity.getVitaleScanStorageKey() != null) {
            identity.setVitaleCard(new PatientIdentity.VitaleCard(entity.getVitaleNirToken(),
                    entity.getVitaleCardVersion(), entity.getVitaleIssuedOn(), entity.getVitaleExpiresOn(),
                    entity.getVitaleReadMode() == null
                            ? PatientIdentity.VitaleReadMode.MANUAL : entity.getVitaleReadMode(),
                    entity.getVitaleScanStorageKey(), entity.getVitaleReadAt()));
        }

        if (entity.getInsurances() != null) {
            entity.getInsurances().stream()
                    .filter(insurance -> insurance.type() == PatientIdentity.InsuranceType.PRINCIPAL)
                    .findFirst()
                    .ifPresent(identity::setPrimaryInsurance);
            entity.getInsurances().stream()
                    .filter(PatientIdentity.HealthInsurance::isComplementary)
                    .findFirst()
                    .ifPresent(identity::setComplementaryInsurance);
        }

        if (entity.isDmpLinked() || entity.getDmpIdentifier() != null) {
            identity.setDmpAccount(new PatientIdentity.DmpAccount(entity.getDmpIdentifier(),
                    entity.isDmpLinked(), entity.isDmpSharingEnabled(), entity.getDmpLinkedAt(),
                    entity.getDmpLastSyncAt()));
        } else {
            identity.setDmpAccount(PatientIdentity.DmpAccount.notLinked());
        }

        identity.setCreatedAt(entity.getCreatedAt());
        identity.setUpdatedAt(entity.getUpdatedAt());
        return identity;
    }

    // ------------------------------------------------------------------
    // PrivacyPreferences
    // ------------------------------------------------------------------

    public PrivacyPreferencesEntity toEntity(PrivacyPreferences preferences) {
        PrivacyPreferencesEntity entity = new PrivacyPreferencesEntity();
        entity.setId(preferences.getId());
        entity.setUserId(preferences.getUserId());
        entity.setPreferredLanguages(new ArrayList<>(preferences.getPreferredLanguages()));
        entity.setPrimaryLanguage(preferences.getPrimaryLanguage());

        PrivacyPreferences.AccessibilitySettings accessibility = preferences.getAccessibility();
        if (accessibility != null) {
            entity.setAccessibilityNeeds(new LinkedHashSet<>(accessibility.needs()));
            entity.setScreenReaderOptimized(accessibility.screenReaderOptimized());
            entity.setLargePrint(accessibility.largePrint());
            entity.setSignLanguage(accessibility.signLanguage());
            entity.setSubtitlesRequired(accessibility.subtitlesRequired());
        }

        PrivacyPreferences.CommunicationSettings communication = preferences.getCommunication();
        if (communication != null) {
            entity.setPreferredChannels(new LinkedHashSet<>(communication.preferredChannels()));
            entity.setQuietHoursEnabled(communication.quietHoursEnabled());
            entity.setQuietHoursStart(communication.quietHoursStart());
            entity.setQuietHoursEnd(communication.quietHoursEnd());
        }

        entity.setConsentHistory(new ArrayList<>(preferences.getConsentHistory()));
        entity.setVisibilityRules(new ArrayList<>(preferences.getVisibilityRules()));
        entity.setDmpSharingEnabled(preferences.isDmpSharingEnabled());
        entity.setErasureRequestedAt(preferences.getErasureRequestedAt());
        entity.setCreatedAt(preferences.getCreatedAt());
        entity.setUpdatedAt(preferences.getUpdatedAt());
        return entity;
    }

    public PrivacyPreferences toDomain(PrivacyPreferencesEntity entity) {
        PrivacyPreferences preferences = new PrivacyPreferences();
        preferences.setId(entity.getId());
        preferences.setUserId(entity.getUserId());
        preferences.setPreferredLanguages(entity.getPreferredLanguages() == null
                ? new ArrayList<>(List.of(PrivacyPreferences.DEFAULT_LANGUAGE))
                : new ArrayList<>(entity.getPreferredLanguages()));
        preferences.setPrimaryLanguage(entity.getPrimaryLanguage() == null
                ? PrivacyPreferences.DEFAULT_LANGUAGE : entity.getPrimaryLanguage());
        preferences.setAccessibility(new PrivacyPreferences.AccessibilitySettings(
                entity.getAccessibilityNeeds(), entity.isScreenReaderOptimized(), entity.isLargePrint(),
                entity.getSignLanguage(), entity.isSubtitlesRequired()));
        preferences.setCommunication(new PrivacyPreferences.CommunicationSettings(
                entity.getPreferredChannels(), entity.isQuietHoursEnabled(), entity.getQuietHoursStart(),
                entity.getQuietHoursEnd()));
        preferences.setConsentHistory(entity.getConsentHistory() == null
                ? new ArrayList<>() : new ArrayList<>(entity.getConsentHistory()));
        preferences.setVisibilityRules(entity.getVisibilityRules() == null
                ? new ArrayList<>() : new ArrayList<>(entity.getVisibilityRules()));
        preferences.setDmpSharingEnabled(entity.isDmpSharingEnabled());
        preferences.setErasureRequestedAt(entity.getErasureRequestedAt());
        preferences.setCreatedAt(entity.getCreatedAt());
        preferences.setUpdatedAt(entity.getUpdatedAt());
        return preferences;
    }
}
