package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.application.port.input.PracticeLocationUseCase;
import com.prdv.rdv.profile.application.port.output.AddressGeocodingPort;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.MedicalFilePolicy;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.application.service.support.ReplacedFileCleaner;
import com.prdv.rdv.profile.config.ProfileProperties;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cas d'usage : lieux d'exercice (module 2.2 — Informations Cabinet).
 *
 * <p>Multi-cabinets, geolocalisation (avec geocodage de secours), horaires,
 * photos, visite virtuelle, accessibilite, parking et transports.
 *
 * <p>Regle de lieu principal : un seul lieu principal a la fois. Le premier lieu
 * cree le devient, la suppression du lieu principal designe un remplacant, et
 * {@link #promoteToMain} change le lieu principal a la demande.
 */
@Service
public class PracticeLocationService implements PracticeLocationUseCase {

    private final PracticeLocationRepository locationRepository;
    private final AddressGeocodingPort geocoding;
    private final MedicalFileStoragePort fileStorage;
    private final ReplacedFileCleaner replacedFileCleaner;
    private final MedicalFilePolicy filePolicy;
    private final CurrentUserPort currentUser;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileProperties properties;
    private final Clock clock;

    public PracticeLocationService(PracticeLocationRepository locationRepository,
                                   AddressGeocodingPort geocoding,
                                   MedicalFileStoragePort fileStorage,
                                   MedicalFilePolicy filePolicy,
                                   CurrentUserPort currentUser,
                                   ProfileViewMapper viewMapper,
                                   ProfileAuditTrail auditTrail,
                                   ProfileProperties properties,
                                   ReplacedFileCleaner replacedFileCleaner,
                                   Clock clock) {
        this.replacedFileCleaner = replacedFileCleaner;
        this.locationRepository = locationRepository;
        this.geocoding = geocoding;
        this.fileStorage = fileStorage;
        this.filePolicy = filePolicy;
        this.currentUser = currentUser;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.PracticeLocationView> myLocations() {
        return locationsOf(currentUser.requireCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.PracticeLocationView> locationsOf(Long practitionerUserId) {
        LocalDateTime now = LocalDateTime.now(clock);
        return locationRepository.findByPractitionerUserId(practitionerUserId).stream()
                .map(location -> viewMapper.locationView(location, now))
                .toList();
    }

    @Override
    @Transactional
    public ProfileViews.PracticeLocationView create(PractitionerProfileCommands.CreateLocation command) {
        Long userId = currentUser.requireCurrentUserId();
        // Le premier lieu d'un praticien devient automatiquement son lieu principal.
        boolean main = command.mainLocation() || locationRepository.findByPractitionerUserId(userId).isEmpty();
        PracticeLocation location = PracticeLocation.create(userId, command.name(), command.line1(),
                command.postalCode(), command.city(), command.country(), main, clock);
        location.setLine2(command.line2());
        applyGeolocation(location, command.latitude(), command.longitude());
        location.replaceOpeningHours(openingHours(command.openingHours()), clock);
        location.updateCoordinates(command.phone(), command.mobilePhone(), command.fax(), command.email(),
                command.website(), command.virtualTourUrl(), clock);
        location.updateAccessibility(command.wheelchairAccessible(), command.parkingAvailable(),
                command.publicTransportInfo(), clock);
        if (command.socialLinks() != null) {
            location.replaceSocialLinks(command.socialLinks(), clock);
        }
        if (main) {
            demoteOtherMainLocations(userId);
        }

        PracticeLocation saved = locationRepository.save(location);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_PROFILE_UPDATED, userId,
                "PracticeLocation", saved.getId(), "lieu d'exercice cree : " + saved.fullAddress());
        return viewMapper.locationView(saved, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public ProfileViews.PracticeLocationView update(String locationId,
                                                    PractitionerProfileCommands.UpdateLocation command) {
        Long userId = currentUser.requireCurrentUserId();
        PracticeLocation location = requireOwned(locationId, userId);

        location.setName(command.name() == null || command.name().isBlank()
                ? location.getName() : command.name().trim());
        location.setLine1(command.line1() == null ? location.getLine1() : command.line1().trim());
        location.setLine2(command.line2());
        location.setPostalCode(command.postalCode() == null
                ? location.getPostalCode() : command.postalCode());
        location.setCity(command.city() == null ? location.getCity() : command.city().trim());
        location.setCountry(command.country() == null ? location.getCountry() : command.country().trim());
        applyGeolocation(location, command.latitude(), command.longitude());
        if (command.openingHours() != null) {
            location.replaceOpeningHours(openingHours(command.openingHours()), clock);
        }
        location.updateCoordinates(command.phone(), command.mobilePhone(), command.fax(), command.email(),
                command.website(), command.virtualTourUrl(), clock);
        location.updateAccessibility(command.wheelchairAccessible(), command.parkingAvailable(),
                command.publicTransportInfo(), clock);
        // Carte fournie : elle remplace les reseaux existants (une carte vide les retire tous).
        if (command.socialLinks() != null) {
            location.replaceSocialLinks(command.socialLinks(), clock);
        }

        PracticeLocation saved = locationRepository.save(location);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_PROFILE_UPDATED, userId,
                "PracticeLocation", saved.getId(), "lieu d'exercice mis a jour");
        return viewMapper.locationView(saved, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public void delete(String locationId) {
        Long userId = currentUser.requireCurrentUserId();
        PracticeLocation location = requireOwned(locationId, userId);
        boolean wasMain = location.isMainLocation();
        // Les photos du cabinet ne sont supprimees qu'une fois la suppression du lieu validee.
        replacedFileCleaner.discardAfterCommit(location.mediaStorageKeys());
        locationRepository.deleteById(locationId);
        if (wasMain) {
            // Le cabinet supprime etait le principal : un autre lieu prend le relais.
            locationRepository.findByPractitionerUserId(userId).stream()
                    .filter(remaining -> !locationId.equals(remaining.getId()))
                    .findFirst()
                    .ifPresent(successor -> {
                        successor.promoteToMain(clock);
                        locationRepository.save(successor);
                    });
        }
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_PROFILE_UPDATED, userId,
                "PracticeLocation", locationId, "lieu d'exercice supprime");
    }

    @Override
    @Transactional
    public ProfileViews.PracticeLocationView promoteToMain(String locationId) {
        Long userId = currentUser.requireCurrentUserId();
        PracticeLocation location = requireOwned(locationId, userId);
        demoteOtherMainLocations(userId);
        location.promoteToMain(clock);
        PracticeLocation saved = locationRepository.save(location);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_PROFILE_UPDATED, userId,
                "PracticeLocation", locationId, "lieu principal modifie");
        return viewMapper.locationView(saved, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public ProfileViews.PracticeLocationView addPhoto(PractitionerProfileCommands.AddLocationPhoto command) {
        Long userId = currentUser.requireCurrentUserId();
        PracticeLocation location = requireOwned(command.locationId(), userId);
        filePolicy.validateImage(command.contentType(), command.content(),
                properties.getMedia().getMaxPhotoBytes());
        MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                properties.getMedia().getLocationFolder() + "/" + command.locationId(),
                command.originalFilename(), command.contentType(), command.content());
        location.addPhoto(new PracticeLocation.Photo(stored.storageKey(), command.type(),
                command.caption()), clock);
        PracticeLocation saved = locationRepository.save(location);
        return viewMapper.locationView(saved, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public void removePhoto(String locationId, String photoId) {
        Long userId = currentUser.requireCurrentUserId();
        PracticeLocation location = requireOwned(locationId, userId);
        PracticeLocation.Photo removed = location.removePhotoById(photoId, clock);
        locationRepository.save(location);
        // Le fichier n'est supprime qu'une fois la transaction validee.
        replacedFileCleaner.discardAfterCommit(List.of(removed.storageKey()));
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.PRACTITIONER_PROFILE_UPDATED, userId,
                "PracticeLocation", locationId, "photo de cabinet supprimee");
    }

    // ------------------------------------------------------------------

    /** Geolocalisation fournie, sinon tentative de geocodage de l'adresse. */
    private void applyGeolocation(PracticeLocation location, Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            location.geolocate(latitude, longitude, clock);
            return;
        }
        geocoding.geocode(location.getLine1(), location.getPostalCode(), location.getCity(),
                location.getCountry()).ifPresent(point ->
                location.geolocate(point.latitude(), point.longitude(), clock));
    }

    private List<PracticeLocation.OpeningHours> openingHours(
            List<PractitionerProfileCommands.OpeningHoursEntry> entries) {
        if (entries == null) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> new PracticeLocation.OpeningHours(entry.day(), entry.opensAt(),
                        entry.closesAt(), entry.closed(), entry.note()))
                .toList();
    }

    private void demoteOtherMainLocations(Long userId) {
        locationRepository.findByPractitionerUserId(userId).forEach(existing -> {
            if (existing.isMainLocation()) {
                existing.demoteFromMain(clock);
                locationRepository.save(existing);
            }
        });
    }

    private PracticeLocation requireOwned(String locationId, Long userId) {
        PracticeLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.LOCATION_NOT_FOUND,
                        "Lieu d'exercice " + locationId + " introuvable"));
        if (!location.getPractitionerUserId().equals(userId)) {
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Ce lieu d'exercice appartient a un autre praticien");
        }
        return location;
    }
}
