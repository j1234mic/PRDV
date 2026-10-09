package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.PatientIdentityCommands;
import com.prdv.rdv.profile.application.command.PrivacyCommands;
import com.prdv.rdv.profile.application.port.input.PatientIdentityUseCase;
import com.prdv.rdv.profile.application.port.input.PrivacyPreferencesUseCase;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.DmpGatewayPort;
import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.application.port.output.PatientBasicsPort;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.port.output.SensitiveDataProtector;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.MediaContentTypes;
import com.prdv.rdv.profile.application.service.support.MedicalFilePolicy;
import com.prdv.rdv.profile.application.service.support.ProfileAccessGuard;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.application.service.support.ReplacedFileCleaner;
import com.prdv.rdv.profile.config.ProfileProperties;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.identity.SocialSecurityNumber;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Cas d'usage « identite patient » (module 2.1 — Informations Personnelles).
 *
 * <p>Orchestration uniquement (Single Responsibility) : les regles de
 * cohesion (un seul contact prefere, une seule adresse par defaut, couverture
 * principale / complementaire) vivent dans l'agregat
 * {@link PatientIdentity}, le stockage et la tokenisation derriere des ports
 * (Dependency Inversion).
 */
@Service
public class PatientIdentityService implements PatientIdentityUseCase {

    private static final String NAMESPACE_NIR = "SOCIAL_SECURITY_NUMBER";
    private static final String NAMESPACE_IDENTITY_DOCUMENT = "IDENTITY_DOCUMENT";
    private static final String PLACEHOLDER_FIRST_NAME = "Patient";
    private static final String PLACEHOLDER_LAST_NAME = "Inconnu";

    private final PatientIdentityRepository identityRepository;
    private final PatientBasicsPort patientBasics;
    private final MedicalFileStoragePort fileStorage;
    private final ReplacedFileCleaner replacedFileCleaner;
    private final SensitiveDataProtector sensitiveData;
    private final DmpGatewayPort dmpGateway;
    private final CurrentUserPort currentUser;
    private final PrivacyPreferencesUseCase privacyPreferences;
    private final ProfileAccessGuard accessGuard;
    private final MedicalFilePolicy filePolicy;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final ProfileProperties properties;
    private final Clock clock;

    public PatientIdentityService(PatientIdentityRepository identityRepository,
                                  PatientBasicsPort patientBasics,
                                  MedicalFileStoragePort fileStorage,
                                  SensitiveDataProtector sensitiveData,
                                  DmpGatewayPort dmpGateway,
                                  CurrentUserPort currentUser,
                                  PrivacyPreferencesUseCase privacyPreferences,
                                  ProfileAccessGuard accessGuard,
                                  MedicalFilePolicy filePolicy,
                                  ProfileViewMapper viewMapper,
                                  ProfileAuditTrail auditTrail,
                                  ProfileEventPublisher eventPublisher,
                                  ProfileProperties properties,
                                  ReplacedFileCleaner replacedFileCleaner,
                                  Clock clock) {
        this.replacedFileCleaner = replacedFileCleaner;
        this.identityRepository = identityRepository;
        this.patientBasics = patientBasics;
        this.fileStorage = fileStorage;
        this.sensitiveData = sensitiveData;
        this.dmpGateway = dmpGateway;
        this.currentUser = currentUser;
        this.privacyPreferences = privacyPreferences;
        this.accessGuard = accessGuard;
        this.filePolicy = filePolicy;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.PatientIdentityView myIdentity() {
        return viewMapper.identityView(viewOrDefault(currentUser.requireCurrentUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.PatientIdentityView identityOf(Long patientUserId) {
        accessGuard.requireAccess(patientUserId, PrivacyPreferences.DataCategory.IDENTITY);
        return viewMapper.identityView(viewOrDefault(patientUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile profilePhoto(Long patientUserId) {
        Long requester = currentUser.requireCurrentUserId();
        Long target = accessGuard.resolveTarget(patientUserId);
        accessGuard.requireAccess(target, PrivacyPreferences.DataCategory.IDENTITY);
        String key = identityRepository.findByUserId(target)
                .map(PatientIdentity::getPhotoStorageKey)
                .orElse(null);
        return downloadMedia(key, requester, target, "photo de profil");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile myProfilePhoto() {
        Long userId = currentUser.requireCurrentUserId();
        String key = identityRepository.findByUserId(userId)
                .map(PatientIdentity::getPhotoStorageKey)
                .orElse(null);
        return downloadMedia(key, userId, userId, "photo de profil");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile myIdentityDocument() {
        Long userId = currentUser.requireCurrentUserId();
        String key = identityRepository.findByUserId(userId)
                .map(PatientIdentity::getIdentityDocumentStorageKey)
                .orElse(null);
        return downloadMedia(key, userId, userId, "piece d'identite");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile myVitaleScan() {
        Long userId = currentUser.requireCurrentUserId();
        String key = identityRepository.findByUserId(userId)
                .map(PatientIdentity::getVitaleCard)
                .map(PatientIdentity.VitaleCard::scanStorageKey)
                .orElse(null);
        return downloadMedia(key, userId, userId, "scan de la carte Vitale");
    }

    // ------------------------------------------------------------------
    // Etat civil et coordonnees
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView updateCivilStatus(PatientIdentityCommands.UpdateCivilStatus command) {
        Long userId = currentUser.requireCurrentUserId();
        PatientIdentity identity = loadOrCreate(userId);
        identity.updateCivilStatus(new PatientIdentity.CivilStatus(command.civility(), command.firstName(),
                command.birthName(), command.lastName(), command.preferredName(), command.birthDate(),
                command.birthPlace(), command.birthCountry(), command.gender(), command.maritalStatus(),
                command.nationality()), clock);
        return saveAndAudit(identity, "etat civil");
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView replaceContacts(PatientIdentityCommands.ReplaceContacts command) {
        PatientIdentity identity = loadOrCreate(currentUser.requireCurrentUserId());
        List<PatientIdentity.ContactPoint> contacts = command.contacts() == null ? List.of()
                : command.contacts().stream()
                .map(entry -> new PatientIdentity.ContactPoint(entry.type(), entry.value(),
                        entry.verified(), entry.preferred()))
                .toList();
        identity.replaceContacts(contacts, clock);
        return saveAndAudit(identity, "contacts (" + contacts.size() + ")");
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView replaceAddresses(PatientIdentityCommands.ReplaceAddresses command) {
        PatientIdentity identity = loadOrCreate(currentUser.requireCurrentUserId());
        List<PatientIdentity.PostalAddress> addresses = command.addresses() == null ? List.of()
                : command.addresses().stream()
                .map(entry -> new PatientIdentity.PostalAddress(entry.type(), entry.line1(), entry.line2(),
                        entry.postalCode(), entry.city(), entry.country(), entry.latitude(),
                        entry.longitude(), entry.isDefault()))
                .toList();
        identity.replaceAddresses(addresses, clock);
        return saveAndAudit(identity, "adresses (" + addresses.size() + ")");
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView updateEmergencyContact(
            PatientIdentityCommands.UpdateEmergencyContact command) {
        PatientIdentity identity = loadOrCreate(currentUser.requireCurrentUserId());
        identity.updateEmergencyContact(new PatientIdentity.EmergencyContact(command.firstName(),
                command.lastName(), command.relationship(), command.phone(), command.email()), clock);
        return saveAndAudit(identity, "personne a prevenir");
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView declareTreatingPhysician(
            PatientIdentityCommands.DeclareTreatingPhysician command) {
        PatientIdentity identity = loadOrCreate(currentUser.requireCurrentUserId());
        identity.declareTreatingPhysician(new PatientIdentity.TreatingPhysician(command.firstName(),
                command.lastName(), command.rppsNumber(), command.phone(), command.email(),
                command.declaredToInsurance()), clock);
        return saveAndAudit(identity, "medecin traitant declare");
    }

    // ------------------------------------------------------------------
    // Couverture sociale
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView registerSocialSecurityNumber(
            PatientIdentityCommands.RegisterSocialSecurityNumber command) {
        PatientIdentity identity = loadOrCreate(currentUser.requireCurrentUserId());
        SocialSecurityNumber nir = SocialSecurityNumber.of(command.value());
        identity.registerSocialSecurityNumber(
                sensitiveData.tokenize(nir.value(), NAMESPACE_NIR), nir.masked(), clock);
        return saveAndAudit(identity, "numero de securite sociale enregistre (" + nir.masked() + ")");
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView updateInsurance(PatientIdentityCommands.UpdateInsurance command) {
        PatientIdentity identity = loadOrCreate(currentUser.requireCurrentUserId());
        identity.updateInsurance(new PatientIdentity.HealthInsurance(command.type(), command.organization(),
                command.memberNumber(), command.contractReference(), command.validUntil()), clock);
        return saveAndAudit(identity, "couverture " + command.type());
    }

    // ------------------------------------------------------------------
    // Fichiers : photo, piece d'identite, carte Vitale
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView uploadProfilePhoto(PatientIdentityCommands.StoreFile command) {
        Long userId = currentUser.requireCurrentUserId();
        PatientIdentity identity = loadOrCreate(userId);
        filePolicy.validateImage(command.contentType(), command.content(),
                properties.getMedia().getMaxPhotoBytes());
        MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                properties.getMedia().getPatientFolder() + "/" + userId + "/photo",
                command.originalFilename(), command.contentType(), command.content());
        String previousKey = identity.getPhotoStorageKey();
        identity.attachPhoto(stored.storageKey(), clock);
        ProfileViews.PatientIdentityView view = saveAndAudit(identity, "photo de profil");
        discardReplacedFile(previousKey, stored.storageKey());
        return view;
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView uploadIdentityDocument(PatientIdentityCommands.StoreFile command) {
        Long userId = currentUser.requireCurrentUserId();
        PatientIdentity identity = loadOrCreate(userId);
        filePolicy.validateDocument(command.contentType(), command.content(),
                properties.getDocuments().getMaxBytes());
        MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                properties.getMedia().getPatientFolder() + "/" + userId + "/identity",
                command.originalFilename(), command.contentType(), command.content());
        String previousKey = identity.getIdentityDocumentStorageKey();
        identity.attachIdentityDocument(stored.storageKey(),
                sensitiveData.tokenize(stored.storageKey(), NAMESPACE_IDENTITY_DOCUMENT), clock);
        ProfileViews.PatientIdentityView view =
                saveAndAudit(identity, "piece d'identite stockee de facon securisee");
        discardReplacedFile(previousKey, stored.storageKey());
        return view;
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView registerVitaleCard(PatientIdentityCommands.RegisterVitaleCard command) {
        Long userId = currentUser.requireCurrentUserId();
        PatientIdentity identity = loadOrCreate(userId);

        String scanKey = null;
        if (command.scanContent() != null && command.scanContent().length > 0) {
            filePolicy.validateImage(command.scanContentType(), command.scanContent(),
                    properties.getMedia().getMaxPhotoBytes());
            MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                    properties.getMedia().getPatientFolder() + "/" + userId + "/vitale",
                    command.scanFilename(), command.scanContentType(), command.scanContent());
            scanKey = stored.storageKey();
        }
        PatientIdentity.VitaleReadMode readMode = command.readMode() == null
                ? (scanKey == null ? PatientIdentity.VitaleReadMode.MANUAL : PatientIdentity.VitaleReadMode.SCAN)
                : command.readMode();

        String nirToken = command.nir() == null || command.nir().isBlank()
                ? null
                : sensitiveData.tokenize(SocialSecurityNumber.of(command.nir()).value(), NAMESPACE_NIR);

        String previousScan = identity.getVitaleCard() == null
                ? null : identity.getVitaleCard().scanStorageKey();
        identity.registerVitaleCard(new PatientIdentity.VitaleCard(nirToken, command.cardVersion(),
                command.issuedOn(), command.expiresOn(), readMode, scanKey, clock.instant()), clock);
        if (nirToken != null) {
            identity.registerSocialSecurityNumber(nirToken,
                    SocialSecurityNumber.of(command.nir()).masked(), clock);
        }
        ProfileViews.PatientIdentityView view = saveAndAudit(identity, "carte Vitale enregistree (" + readMode + ")");
        discardReplacedFile(previousScan, scanKey);
        return view;
    }

    // ------------------------------------------------------------------
    // DMP
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView linkDmp(PatientIdentityCommands.LinkDmp command) {
        Long userId = currentUser.requireCurrentUserId();
        PatientIdentity identity = loadOrCreate(userId);
        String dmpIdentifier = command.dmpIdentifier() != null && !command.dmpIdentifier().isBlank()
                ? command.dmpIdentifier()
                : dmpGateway.linkPatient(userId, identity.getSocialSecurityNumberToken());
        identity.linkDmp(dmpIdentifier, command.sharingEnabled(), clock);
        // Le consentement au partage DMP est la responsabilite du cas d'usage confidentialite.
        privacyPreferences.recordConsent(new PrivacyCommands.RecordConsent(
                PrivacyPreferences.ConsentPurpose.DMP_SHARING, command.sharingEnabled(), null, null));
        return saveAndAudit(identity, "DMP rattache : " + dmpIdentifier);
    }

    @Override
    @Transactional
    public ProfileViews.PatientIdentityView updateDmpSharing(PatientIdentityCommands.UpdateDmpSharing command) {
        Long userId = currentUser.requireCurrentUserId();
        PatientIdentity identity = loadOrCreate(userId);
        PatientIdentity.DmpAccount dmp = identity.getDmpAccount();
        if (dmp == null || !dmp.linked()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Aucun DMP rattache : impossible de modifier le partage");
        }
        // Consentement, passerelle DMP et drapeau de partage sont mis a jour au meme endroit.
        privacyPreferences.updateDmpSharing(new PrivacyCommands.UpdateDmpSharing(
                command.sharingEnabled(), null, null));
        PatientIdentity reloaded = loadOrCreate(userId);
        return saveAndAudit(reloaded,
                "partage DMP " + (command.sharingEnabled() ? "active" : "desactive"));
    }

    @Override
    @Transactional
    public ProfileViews.DmpAccountView synchronizeDmp() {
        Long userId = currentUser.requireCurrentUserId();
        PatientIdentity identity = loadOrCreate(userId);
        PatientIdentity.DmpAccount dmp = identity.getDmpAccount();
        if (dmp == null || !dmp.linked()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Aucun DMP rattache : synchronisation impossible");
        }
        int pulled = dmpGateway.pullUpdates(userId, dmp.dmpIdentifier());
        identity.markDmpSynced(clock);
        PatientIdentity saved = identityRepository.save(identity);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PATIENT_PROFILE_UPDATED, userId,
                "synchronisation DMP (" + pulled + " elements)");
        return viewMapper.dmpView(saved.getDmpAccount());
    }

    // ------------------------------------------------------------------

    /** Vue sans effet de bord : une identite absente est presentee par defaut, sans etre enregistree. */
    private PatientIdentity viewOrDefault(Long userId) {
        return identityRepository.findByUserId(userId)
                .orElseGet(() -> PatientIdentity.create(userId, initialCivilStatus(userId), clock));
    }

    /** Lecture controlee d'un fichier d'identite ; le droit d'acces est verifie par l'appelant. */
    private ProfileViews.DocumentFile downloadMedia(String storageKey, Long requester, Long owner, String label) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND, "Aucun fichier enregistre : " + label);
        }
        byte[] content = fileStorage.retrieve(storageKey);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDIA_DOWNLOADED, requester,
                "PatientIdentity", String.valueOf(owner), label + " (" + content.length + " octets)");
        return new ProfileViews.DocumentFile(MediaContentTypes.filenameOf(storageKey),
                MediaContentTypes.contentTypeOf(storageKey), content.length, content);
    }

    /** Supprime le fichier remplace une fois la transaction validee (au mieux). */
    private void discardReplacedFile(String previousKey, String currentKey) {
        replacedFileCleaner.discardReplaced(previousKey, currentKey);
    }

    /** Charge l'identite, ou l'amorce a partir des elements connus du contexte IAM. */
    private PatientIdentity loadOrCreate(Long userId) {
        return identityRepository.findByUserId(userId)
                .orElseGet(() -> identityRepository.save(
                        PatientIdentity.create(userId, initialCivilStatus(userId), clock)));
    }

    private PatientIdentity.CivilStatus initialCivilStatus(Long userId) {
        return patientBasics.basicsOf(userId)
                .map(basics -> new PatientIdentity.CivilStatus(null,
                        defaultsTo(basics.firstName(), PLACEHOLDER_FIRST_NAME),
                        null,
                        defaultsTo(basics.lastName(), PLACEHOLDER_LAST_NAME),
                        null, basics.birthDate(), null, null,
                        parseGender(basics.gender()), null, null))
                .orElseGet(() -> new PatientIdentity.CivilStatus(null, PLACEHOLDER_FIRST_NAME, null,
                        PLACEHOLDER_LAST_NAME, null, null, null, null,
                        PatientIdentity.Gender.UNSPECIFIED, null, null));
    }

    private static String defaultsTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static PatientIdentity.Gender parseGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return PatientIdentity.Gender.UNSPECIFIED;
        }
        try {
            return PatientIdentity.Gender.valueOf(gender.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PatientIdentity.Gender.UNSPECIFIED;
        }
    }

    private ProfileViews.PatientIdentityView saveAndAudit(PatientIdentity identity, String detail) {
        PatientIdentity saved = identityRepository.save(identity);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PATIENT_PROFILE_UPDATED,
                saved.getUserId(), "PatientIdentity", String.valueOf(saved.getId()), detail);
        eventPublisher.publish(new ProfileEvent.PatientIdentityUpdated(saved.getUserId(), Instant.now(clock)));
        return viewMapper.identityView(saved);
    }
}
