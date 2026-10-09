package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.application.port.input.PractitionerProfileUseCase;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerDossierRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerRatingRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerVerificationPort;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.port.output.SensitiveDataProtector;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.MedicalFilePolicy;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.application.service.support.ReplacedFileCleaner;
import com.prdv.rdv.profile.config.ProfileProperties;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.practitioner.Badge;
import com.prdv.rdv.profile.domain.model.practitioner.BadgePolicy;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerRating;
import com.prdv.rdv.profile.domain.model.practitioner.RatingSummary;
import com.prdv.rdv.profile.domain.model.practitioner.Siret;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Cas d'usage : dossier professionnel du praticien (module 2.2).
 *
 * <p>Identite professionnelle, informations medicales (secteur, tarifs,
 * OPTAM, tiers-payant, actes, equipements, ages acceptes, teleconsultation),
 * gestion (SIRET valide par Luhn, RIB tokenise, RC pro, URSSAF, AGA/CGA,
 * cotisation ordinale, accreditations), reseau et visibilite.
 *
 * <p>Les badges ne sont pas stockes : ils sont recalcules a la demande par la
 * {@link BadgePolicy} a partir de faits verifiables (verifications, avis,
 * anciennete), ce qui interdit toute donnee derivee obsolete.
 */
@Service
public class PractitionerProfileService implements PractitionerProfileUseCase {

    private static final String NAMESPACE_IBAN = "PRACTITIONER_IBAN";

    private final PractitionerDossierRepository dossierRepository;
    private final PractitionerRatingRepository ratingRepository;
    private final PracticeLocationRepository locationRepository;
    private final PractitionerVerificationPort verificationPort;
    private final MedicalFileStoragePort fileStorage;
    private final ReplacedFileCleaner replacedFileCleaner;
    private final SensitiveDataProtector sensitiveData;
    private final MedicalFilePolicy filePolicy;
    private final CurrentUserPort currentUser;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final ProfileProperties properties;
    private final Clock clock;

    public PractitionerProfileService(PractitionerDossierRepository dossierRepository,
                                      PractitionerRatingRepository ratingRepository,
                                      PracticeLocationRepository locationRepository,
                                      PractitionerVerificationPort verificationPort,
                                      MedicalFileStoragePort fileStorage,
                                      SensitiveDataProtector sensitiveData,
                                      MedicalFilePolicy filePolicy,
                                      CurrentUserPort currentUser,
                                      ProfileViewMapper viewMapper,
                                      ProfileAuditTrail auditTrail,
                                      ProfileEventPublisher eventPublisher,
                                      ProfileProperties properties,
                                      ReplacedFileCleaner replacedFileCleaner,
                                      Clock clock) {
        this.replacedFileCleaner = replacedFileCleaner;
        this.dossierRepository = dossierRepository;
        this.ratingRepository = ratingRepository;
        this.locationRepository = locationRepository;
        this.verificationPort = verificationPort;
        this.fileStorage = fileStorage;
        this.sensitiveData = sensitiveData;
        this.filePolicy = filePolicy;
        this.currentUser = currentUser;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.PractitionerDossierView myDossier() {
        return view(currentUser.requireCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.PractitionerDossierView dossierOf(Long practitionerUserId) {
        return view(practitionerUserId);
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView updateIdentity(PractitionerProfileCommands.UpdateIdentity command) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = dossierRepository.findByUserId(userId)
                .orElseGet(() -> PractitionerDossier.create(userId, command.identity(), clock));
        dossier.updateIdentity(mergeMedia(command.identity(), dossier.getIdentity()), clock);
        return saveAndAudit(dossier, "identite professionnelle");
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView updatePracticeInformation(
            PractitionerProfileCommands.UpdatePracticeInformation command) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        dossier.updatePracticeInformation(command.information(), clock);
        return saveAndAudit(dossier, "informations medicales (secteur, tarifs, actes)");
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView updateManagement(
            PractitionerProfileCommands.UpdateManagement command) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        PractitionerDossier.ProfessionalManagement source = command.management();

        String siret = source.siret() == null || source.siret().isBlank()
                ? null
                : Siret.of(source.siret()).value();
        String ribToken = command.iban() == null || command.iban().isBlank()
                ? source.ribToken()
                : sensitiveData.tokenize(command.iban(), NAMESPACE_IBAN);
        String maskedIban = command.iban() == null || command.iban().isBlank()
                ? source.maskedIban()
                : sensitiveData.mask(command.iban());

        dossier.updateManagement(new PractitionerDossier.ProfessionalManagement(siret, ribToken, maskedIban,
                source.professionalInsurer(), source.insurancePolicyNumber(), source.insuranceExpiresOn(),
                source.urssafNumber(), source.accountingAssociation(),
                source.accountingAssociationNumber(), source.ordinalCotisationYear(),
                source.ordinalCotisationAmount(), source.qualityCertifications(), source.accreditations()),
                clock);
        return saveAndAudit(dossier, "gestion professionnelle (SIRET, RIB, RC pro, ordre)");
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView updateVisibility(
            PractitionerProfileCommands.UpdateVisibility command) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        dossier.updateVisibility(command.visibility(), clock);
        return saveAndAudit(dossier, "visibilite et marketing");
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView addNetworkContact(
            PractitionerProfileCommands.AddNetworkContact command) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        dossier.addNetworkContact(command.role(), command.name(), command.specialty(),
                command.registrationNumber(), command.city(), clock);
        return saveAndAudit(dossier, "contact de reseau ajoute (" + command.role() + ")");
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView removeNetworkContact(String contactId) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        dossier.removeNetworkContact(contactId, clock);
        return saveAndAudit(dossier, "contact de reseau retire");
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView uploadPhoto(PractitionerProfileCommands.StoreMedia command) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        filePolicy.validateImage(command.contentType(), command.content(),
                properties.getMedia().getMaxPhotoBytes());
        MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                properties.getMedia().getPractitionerFolder() + "/" + userId + "/photo",
                command.originalFilename(), command.contentType(), command.content());
        String previousKey = dossier.getIdentity().photoStorageKey();
        dossier.updateIdentity(dossier.getIdentity().withPhoto(stored.storageKey()), clock);
        ProfileViews.PractitionerDossierView view = saveAndAudit(dossier, "photo professionnelle");
        replacedFileCleaner.discardReplaced(previousKey, stored.storageKey());
        return view;
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerDossierView uploadPresentationVideo(
            PractitionerProfileCommands.StoreMedia command) {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        filePolicy.validateVideo(command.contentType(), command.content(),
                properties.getMedia().getMaxVideoBytes());
        MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                properties.getMedia().getPractitionerFolder() + "/" + userId + "/video",
                command.originalFilename(), command.contentType(), command.content());
        String previousKey = dossier.getIdentity().presentationVideoKey();
        dossier.updateIdentity(dossier.getIdentity().withPresentationVideo(stored.storageKey()), clock);
        ProfileViews.PractitionerDossierView view = saveAndAudit(dossier, "video de presentation");
        replacedFileCleaner.discardReplaced(previousKey, stored.storageKey());
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.PractitionerDossierView refreshBadges() {
        Long userId = currentUser.requireCurrentUserId();
        PractitionerDossier dossier = requireDossier(userId);
        List<Badge> badges = currentBadges(userId, dossier);
        badges.forEach(badge -> eventPublisher.publish(new ProfileEvent.PractitionerBadgeGranted(userId,
                badge.type().name(), badge.reason(), clock.instant())));
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_BADGES_REFRESHED, userId,
                "PractitionerDossier", String.valueOf(dossier.getId()),
                "badges : " + badges.stream().map(badge -> badge.type().name()).toList());
        return viewMapper.dossierView(dossier, ratingSummary(userId), badges,
                locationRepository.findByPractitionerUserId(userId));
    }

    // ------------------------------------------------------------------

    ProfileViews.PractitionerDossierView view(Long practitionerUserId) {
        PractitionerDossier dossier = requireDossier(practitionerUserId);
        return viewMapper.dossierView(dossier, ratingSummary(practitionerUserId),
                currentBadges(practitionerUserId, dossier),
                locationRepository.findByPractitionerUserId(practitionerUserId));
    }

    RatingSummary ratingSummary(Long practitionerUserId) {
        List<PractitionerRating> visible = ratingRepository.findByPractitionerUserId(practitionerUserId)
                .stream()
                .filter(rating -> !rating.isHidden())
                .toList();
        return RatingSummary.of(visible);
    }

    List<Badge> currentBadges(Long practitionerUserId, PractitionerDossier dossier) {
        return BadgePolicy.evaluate(dossier, ratingSummary(practitionerUserId),
                verificationPort.verificationOf(practitionerUserId),
                verificationPort.registeredAt(practitionerUserId).orElse(null), clock);
    }

    /**
     * Une mise a jour de l'identite ne doit jamais effacer la photo ou la video
     * deja televersees : ces cles ne sont pas fournies par le formulaire.
     */
    private static PractitionerDossier.ProfessionalIdentity mergeMedia(
            PractitionerDossier.ProfessionalIdentity incoming,
            PractitionerDossier.ProfessionalIdentity previous) {
        if (previous == null) {
            return incoming;
        }
        PractitionerDossier.ProfessionalIdentity merged = incoming;
        if (isBlank(incoming.photoStorageKey()) && !isBlank(previous.photoStorageKey())) {
            merged = merged.withPhoto(previous.photoStorageKey());
        }
        if (isBlank(incoming.presentationVideoKey()) && !isBlank(previous.presentationVideoKey())) {
            merged = merged.withPresentationVideo(previous.presentationVideoKey());
        }
        return merged;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private PractitionerDossier requireDossier(Long userId) {
        return dossierRepository.findByUserId(userId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.PRACTITIONER_PROFILE_INCOMPLETE,
                        "Dossier professionnel inexistant : renseignez d'abord votre identite"
                                + " professionnelle (PUT /api/v1/profile/practitioner/me/identity)"));
    }

    private ProfileViews.PractitionerDossierView saveAndAudit(PractitionerDossier dossier, String section) {
        PractitionerDossier saved = dossierRepository.save(dossier);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_PROFILE_UPDATED,
                saved.getUserId(), "PractitionerDossier", String.valueOf(saved.getId()), section);
        eventPublisher.publish(new ProfileEvent.PractitionerProfileUpdated(saved.getUserId(), section,
                clock.instant()));
        return viewMapper.dossierView(saved, ratingSummary(saved.getUserId()),
                currentBadges(saved.getUserId(), saved),
                locationRepository.findByPractitionerUserId(saved.getUserId()));
    }
}
