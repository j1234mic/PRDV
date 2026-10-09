package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.application.port.input.PractitionerDirectoryUseCase;
import com.prdv.rdv.profile.application.port.input.PractitionerProfileUseCase;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerDossierRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerRatingRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerVerificationPort;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.MediaContentTypes;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.practitioner.Badge;
import com.prdv.rdv.profile.domain.model.practitioner.BadgePolicy;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerRating;
import com.prdv.rdv.profile.domain.model.practitioner.RatingSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Cas d'usage : annuaire public des praticiens, avis patients et badges.
 *
 * <p>La recherche est publique (fiche praticien, cabinet, tarifs) ; les avis
 * sont ecrits par des patients authentifies, une seule fois par praticien, et
 * les avis masques par moderation ne sont jamais exposes. Les medias publics
 * (photo, video, photos de cabinet) ne sont servis que pour une fiche publiable.
 */
@Service
public class PractitionerDirectoryService implements PractitionerDirectoryUseCase {

    private static final int MAX_PAGE_SIZE = 50;
    /** Plafond du parcours applique quand un filtre sur badge est demande (badge calcule, non indexe). */
    private static final int MAX_BADGE_SCAN = 500;

    private final PractitionerDossierRepository dossierRepository;
    private final PracticeLocationRepository locationRepository;
    private final PractitionerRatingRepository ratingRepository;
    private final PractitionerVerificationPort verificationPort;
    private final PractitionerProfileUseCase practitionerProfile;
    private final CurrentUserPort currentUser;
    private final MedicalFileStoragePort fileStorage;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final Clock clock;

    public PractitionerDirectoryService(PractitionerDossierRepository dossierRepository,
                                        PracticeLocationRepository locationRepository,
                                        PractitionerRatingRepository ratingRepository,
                                        PractitionerVerificationPort verificationPort,
                                        PractitionerProfileUseCase practitionerProfile,
                                        CurrentUserPort currentUser,
                                        MedicalFileStoragePort fileStorage,
                                        ProfileViewMapper viewMapper,
                                        ProfileAuditTrail auditTrail,
                                        ProfileEventPublisher eventPublisher,
                                        Clock clock) {
        this.dossierRepository = dossierRepository;
        this.locationRepository = locationRepository;
        this.ratingRepository = ratingRepository;
        this.verificationPort = verificationPort;
        this.practitionerProfile = practitionerProfile;
        this.currentUser = currentUser;
        this.fileStorage = fileStorage;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.PractitionerDossierView publicProfile(Long practitionerUserId) {
        return practitionerProfile.dossierOf(practitionerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.PagedResult<ProfileViews.DirectoryEntryView> search(String term,
                                                                            String specialty,
                                                                            String city,
                                                                            boolean teleconsultationOnly,
                                                                            boolean wheelchairAccessibleOnly,
                                                                            Badge.BadgeType badge,
                                                                            int page,
                                                                            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;

        if (badge != null || wheelchairAccessibleOnly) {
            // Badge (derive, jamais stocke) et accessibilite (croisee avec les lieux) : parcours borne
            // puis pagination en memoire, pour que le total corresponde toujours aux resultats.
            List<ProfileViews.DirectoryEntryView> scanned = scanCandidates(term, specialty, city,
                    teleconsultationOnly, wheelchairAccessibleOnly).stream()
                    .filter(entry -> badge == null || entry.badges().contains(badge))
                    .toList();
            return new ProfileViews.PagedResult<>(scanned.stream().skip(offset).limit(safeSize).toList(),
                    scanned.size(), safePage, safeSize);
        }

        Optional<List<Long>> candidatesByCity = resolveCandidates(city, false);
        if (candidatesByCity.isPresent()) {
            List<ProfileViews.DirectoryEntryView> matching = new ArrayList<>();
            for (Long practitionerUserId : candidatesByCity.get()) {
                Optional<PractitionerDossier> dossier = dossierRepository.findByUserId(practitionerUserId);
                if (dossier.isEmpty() || !matches(dossier.get(), term, specialty, teleconsultationOnly)) {
                    continue;
                }
                matching.add(entryView(dossier.get()));
            }
            List<ProfileViews.DirectoryEntryView> items = matching.stream()
                    .skip(offset).limit(safeSize).toList();
            return new ProfileViews.PagedResult<>(items, matching.size(), safePage, safeSize);
        }

        List<ProfileViews.DirectoryEntryView> items = dossierRepository
                .search(term, specialty, teleconsultationOnly, offset, safeSize).stream()
                .map(this::entryView)
                .toList();
        long total = dossierRepository.countSearch(term, specialty, teleconsultationOnly);
        return new ProfileViews.PagedResult<>(items, total, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.PractitionerRatingView> ratingsOf(Long practitionerUserId) {
        Long requester = currentUser.currentUserId().orElse(null);
        boolean isOwner = requester != null && requester.equals(practitionerUserId);
        return ratingRepository.findByPractitionerUserId(practitionerUserId).stream()
                .filter(rating -> !rating.isHidden())
                .map(rating -> new ProfileViews.PractitionerRatingView(rating.getId(),
                        isOwner ? rating.getPatientUserId() : null, rating.getScore(), rating.getComment(),
                        rating.getCreatedAt(), rating.isHidden()))
                .toList();
    }

    @Override
    @Transactional
    public ProfileViews.PractitionerRatingView submitRating(Long practitionerUserId,
                                                            PractitionerProfileCommands.SubmitRating command) {
        Long userId = currentUser.requireCurrentUserId();
        ratingRepository.findByPractitionerAndPatient(practitionerUserId, userId).ifPresent(existing -> {
            throw ProfileException.of(ProfileErrorCode.RATING_ALREADY_SUBMITTED,
                    "Vous avez deja donne un avis sur ce praticien");
        });

        PractitionerRating rating = PractitionerRating.submit(practitionerUserId, userId, command.score(),
                command.comment(), clock);
        PractitionerRating saved = ratingRepository.save(rating);

        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_RATED, userId,
                "PractitionerRating", saved.getId(), "avis " + command.score() + "/5 sur le praticien "
                        + practitionerUserId);
        eventPublisher.publish(new ProfileEvent.PractitionerRated(practitionerUserId, userId,
                command.score(), clock.instant()));
        return viewMapper.ratingView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile practitionerPhoto(Long practitionerUserId) {
        PractitionerDossier dossier = publicDossier(practitionerUserId);
        String key = dossier.getIdentity() == null ? null : dossier.getIdentity().photoStorageKey();
        return mediaFile(key, "photo professionnelle");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile presentationVideo(Long practitionerUserId) {
        PractitionerDossier dossier = publicDossier(practitionerUserId);
        String key = dossier.getIdentity() == null ? null : dossier.getIdentity().presentationVideoKey();
        return mediaFile(key, "video de presentation");
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile locationPhoto(Long practitionerUserId, String locationId, String photoId) {
        publicDossier(practitionerUserId);
        PracticeLocation location = locationRepository.findById(locationId)
                .filter(candidate -> practitionerUserId.equals(candidate.getPractitionerUserId()))
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.LOCATION_NOT_FOUND,
                        "Lieu d'exercice introuvable pour ce praticien"));
        PracticeLocation.Photo photo = location.findPhoto(photoId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                        "Photo de cabinet introuvable"));
        return mediaFile(photo.storageKey(), "photo de cabinet");
    }

    // ------------------------------------------------------------------

    /** Seule une fiche publiable expose ses medias : sinon le media est traite comme inexistant. */
    private PractitionerDossier publicDossier(Long practitionerUserId) {
        return dossierRepository.findByUserId(practitionerUserId)
                .filter(PractitionerDossier::isPublishable)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.PROFILE_NOT_FOUND,
                        "Aucune fiche publique pour ce praticien"));
    }

    private ProfileViews.DocumentFile mediaFile(String storageKey, String label) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND, "Aucun fichier enregistre : " + label);
        }
        byte[] content = fileStorage.retrieve(storageKey);
        return new ProfileViews.DocumentFile(MediaContentTypes.filenameOf(storageKey),
                MediaContentTypes.contentTypeOf(storageKey), content.length, content);
    }

    /**
     * Fiches correspondant aux criteres, plafonnees a {@link #MAX_BADGE_SCAN}.
     * Utilise lorsqu'un filtre sur badge ou sur accessibilite PMR est demande.
     */
    private List<ProfileViews.DirectoryEntryView> scanCandidates(String term, String specialty, String city,
                                                                 boolean teleconsultationOnly,
                                                                 boolean wheelchairAccessibleOnly) {
        List<ProfileViews.DirectoryEntryView> entries = new ArrayList<>();
        Optional<List<Long>> candidatesByCity = resolveCandidates(city, wheelchairAccessibleOnly);
        if (candidatesByCity.isPresent()) {
            for (Long practitionerUserId : candidatesByCity.get()) {
                if (entries.size() >= MAX_BADGE_SCAN) {
                    break;
                }
                Optional<PractitionerDossier> dossier = dossierRepository.findByUserId(practitionerUserId);
                if (dossier.isEmpty() || !matches(dossier.get(), term, specialty, teleconsultationOnly)) {
                    continue;
                }
                if (wheelchairAccessibleOnly && !hasAccessibleLocation(practitionerUserId)) {
                    continue;
                }
                entries.add(entryView(dossier.get()));
            }
        } else {
            dossierRepository.search(term, specialty, teleconsultationOnly, 0, MAX_BADGE_SCAN).stream()
                    .filter(dossier -> !wheelchairAccessibleOnly || hasAccessibleLocation(dossier.getUserId()))
                    .map(this::entryView)
                    .forEach(entries::add);
        }
        return entries;
    }

    /** Identifiants des praticiens exerçant dans la ville demandee (ordre stable). */
    private Optional<List<Long>> resolveCandidates(String city, boolean wheelchairAccessibleOnly) {
        if (city == null || city.isBlank()) {
            return Optional.empty();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (PracticeLocation location : locationRepository.findByCity(city.trim())) {
            if (wheelchairAccessibleOnly && !location.isWheelchairAccessible()) {
                continue;
            }
            ids.add(location.getPractitionerUserId());
        }
        return Optional.of(List.copyOf(ids));
    }

    private boolean hasAccessibleLocation(Long practitionerUserId) {
        return locationRepository.findByPractitionerUserId(practitionerUserId).stream()
                .anyMatch(PracticeLocation::isWheelchairAccessible);
    }

    private boolean matches(PractitionerDossier dossier, String term, String specialty,
                            boolean teleconsultationOnly) {
        PractitionerDossier.ProfessionalIdentity identity = dossier.getIdentity();
        if (identity == null || !dossier.isPublishable()) {
            return false;
        }
        if (teleconsultationOnly && (dossier.getPracticeInformation() == null
                || !dossier.getPracticeInformation().teleconsultation())) {
            return false;
        }
        if (specialty != null && !specialty.isBlank()
                && !equalsIgnoreCase(identity.mainSpecialty(), specialty)) {
            return false;
        }
        if (term == null || term.isBlank()) {
            return true;
        }
        String needle = term.trim().toLowerCase(Locale.ROOT);
        return contains(identity.fullName(), needle)
                || contains(identity.mainSpecialty(), needle)
                || identity.skills().stream().anyMatch(skill -> contains(skill, needle))
                || identity.subSpecialties().stream().anyMatch(sub -> contains(sub, needle));
    }

    private static boolean equalsIgnoreCase(String value, String expected) {
        return value != null && value.trim().equalsIgnoreCase(expected.trim());
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private ProfileViews.DirectoryEntryView entryView(PractitionerDossier dossier) {
        List<PracticeLocation> locations = locationRepository.findByPractitionerUserId(dossier.getUserId());
        String city = locations.stream()
                .filter(PracticeLocation::isMainLocation)
                .map(PracticeLocation::getCity)
                .findFirst()
                .orElseGet(() -> locations.stream().map(PracticeLocation::getCity).findFirst().orElse(null));
        boolean accessible = locations.stream().anyMatch(PracticeLocation::isWheelchairAccessible);

        RatingSummary summary = RatingSummary.of(ratingRepository
                .findByPractitionerUserId(dossier.getUserId()).stream()
                .filter(rating -> !rating.isHidden())
                .toList());
        List<Badge> badges = BadgePolicy.evaluate(dossier, summary,
                verificationPort.verificationOf(dossier.getUserId()),
                verificationPort.registeredAt(dossier.getUserId()).orElse(null), clock);
        return viewMapper.directoryEntryView(dossier, summary, viewMapper.activeBadgeTypes(badges), city,
                accessible);
    }
}
