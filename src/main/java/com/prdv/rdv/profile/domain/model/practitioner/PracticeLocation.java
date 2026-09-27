package com.prdv.rdv.profile.domain.model.practitioner;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Agregat « Lieu d'exercice » (module 2.2 — Informations Cabinet).
 *
 * <p>Un praticien peut exercer dans plusieurs lieux : adresse complete avec
 * geolocalisation, photos (salle d'attente, equipements), visite virtuelle
 * 360 degres, horaires d'ouverture, coordonnees professionnelles (fixe,
 * mobile, fax, email), site web, reseaux sociaux, accessibilite handicapes,
 * parking et transports en commun.
 */
@Getter
@Setter
public class PracticeLocation {

    public enum PhotoType { WAITING_ROOM, CONSULTATION_ROOM, EQUIPMENT, EXTERIOR, RECEPTION }

    /** Creneau d'ouverture pour un jour donne. */
    public record OpeningHours(DayOfWeek day, LocalTime opensAt, LocalTime closesAt,
                               boolean closed, String note) {

        public OpeningHours {
            if (day == null) {
                throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                        "Le jour d'ouverture est obligatoire");
            }
            if (!closed) {
                if (opensAt == null || closesAt == null) {
                    throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                            "Un jour ouvert exige une heure d'ouverture et de fermeture");
                }
                if (!closesAt.isAfter(opensAt)) {
                    throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                            "L'heure de fermeture doit suivre l'heure d'ouverture");
                }
            }
        }

        public static OpeningHours closedDay(DayOfWeek day) {
            return new OpeningHours(day, null, null, true, null);
        }

        public boolean covers(LocalDateTime moment) {
            return !closed && moment.getDayOfWeek() == day
                    && !moment.toLocalTime().isBefore(opensAt)
                    && moment.toLocalTime().isBefore(closesAt);
        }
    }

    public record Photo(String storageKey, PhotoType type, String caption) {

        public Photo {
            if (storageKey == null || storageKey.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                        "La cle de stockage de la photo est obligatoire");
            }
            if (type == null) {
                throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                        "Le type de photo est obligatoire");
            }
        }
    }

    private static final int MAX_PHOTOS = 20;

    private String id;
    private Long practitionerUserId;
    private String name;
    private boolean mainLocation;

    private String line1;
    private String line2;
    private String postalCode;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;

    private List<OpeningHours> openingHours = new ArrayList<>();
    private List<Photo> photos = new ArrayList<>();
    private Map<String, String> socialLinks = new LinkedHashMap<>();

    private String virtualTourUrl;
    private String phone;
    private String mobilePhone;
    private String fax;
    private String email;
    private String website;

    private boolean wheelchairAccessible;
    private boolean parkingAvailable;
    private String publicTransportInfo;

    private Instant createdAt;
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Fabrique
    // ------------------------------------------------------------------

    public static PracticeLocation create(Long practitionerUserId, String name, String line1,
                                          String postalCode, String city, String country,
                                          boolean mainLocation, Clock clock) {
        if (practitionerUserId == null) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                    "Le lieu d'exercice doit etre rattache a un praticien");
        }
        if (line1 == null || line1.isBlank() || city == null || city.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                    "L'adresse du lieu d'exercice est incomplete");
        }
        PracticeLocation location = new PracticeLocation();
        location.id = UUID.randomUUID().toString();
        location.practitionerUserId = practitionerUserId;
        location.name = name == null || name.isBlank() ? "Cabinet" : name.trim();
        location.line1 = line1.trim();
        location.postalCode = postalCode;
        location.city = city.trim();
        location.country = country == null || country.isBlank() ? "FR" : country.trim();
        location.mainLocation = mainLocation;
        location.createdAt = clock.instant();
        location.updatedAt = location.createdAt;
        return location;
    }

    // ------------------------------------------------------------------
    // Comportements
    // ------------------------------------------------------------------

    public void geolocate(Double newLatitude, Double newLongitude, Clock clock) {
        if (newLatitude != null && (newLatitude < -90 || newLatitude > 90)) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID, "Latitude invalide : " + newLatitude);
        }
        if (newLongitude != null && (newLongitude < -180 || newLongitude > 180)) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID, "Longitude invalide : " + newLongitude);
        }
        this.latitude = newLatitude;
        this.longitude = newLongitude;
        touch(clock);
    }

    public void updateCoordinates(String newPhone, String newMobilePhone, String newFax, String newEmail,
                                  String newWebsite, String newVirtualTourUrl, Clock clock) {
        this.phone = newPhone;
        this.mobilePhone = newMobilePhone;
        this.fax = newFax;
        this.email = newEmail;
        this.website = newWebsite;
        this.virtualTourUrl = newVirtualTourUrl;
        touch(clock);
    }

    public void updateAccessibility(boolean accessible, boolean parking, String transportInfo, Clock clock) {
        this.wheelchairAccessible = accessible;
        this.parkingAvailable = parking;
        this.publicTransportInfo = transportInfo;
        touch(clock);
    }

    /** Remplace la semaine type d'ouverture (au plus un creneau par jour pour rester lisible). */
    public void replaceOpeningHours(List<OpeningHours> hours, Clock clock) {
        List<OpeningHours> cleaned = new ArrayList<>();
        if (hours != null) {
            for (OpeningHours slot : hours) {
                boolean alreadyPresent = cleaned.stream().anyMatch(existing -> existing.day() == slot.day());
                if (alreadyPresent) {
                    throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                            "Plusieurs creneaux sont declares pour le jour " + slot.day());
                }
                cleaned.add(slot);
            }
        }
        this.openingHours = cleaned;
        touch(clock);
    }

    public Optional<OpeningHours> hoursFor(DayOfWeek day) {
        return openingHours.stream().filter(slot -> slot.day() == day).findFirst();
    }

    /** Le cabinet est-il ouvert a cet instant ? */
    public boolean isOpenAt(LocalDateTime moment) {
        return openingHours.stream().anyMatch(slot -> slot.covers(moment));
    }

    public Photo addPhoto(Photo photo, Clock clock) {
        if (photos.size() >= MAX_PHOTOS) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                    "Trop de photos pour ce lieu d'exercice (maximum " + MAX_PHOTOS + ")");
        }
        this.photos.add(photo);
        touch(clock);
        return photo;
    }

    public void removePhoto(String storageKey, Clock clock) {
        boolean removed = this.photos.removeIf(photo -> photo.storageKey().equals(storageKey));
        if (!removed) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                    "Aucune photo ne correspond a la cle " + storageKey);
        }
        touch(clock);
    }

    public void addSocialLink(String platform, String url, Clock clock) {
        if (platform == null || platform.isBlank() || url == null || url.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                    "Un lien de reseau social exige une plateforme et une URL");
        }
        this.socialLinks.put(platform.trim().toLowerCase(java.util.Locale.ROOT), url.trim());
        touch(clock);
    }

    public void promoteToMain(Clock clock) {
        this.mainLocation = true;
        touch(clock);
    }

    public String fullAddress() {
        StringBuilder builder = new StringBuilder(line1);
        if (line2 != null && !line2.isBlank()) {
            builder.append(", ").append(line2);
        }
        if (postalCode != null && !postalCode.isBlank()) {
            builder.append(", ").append(postalCode);
        }
        builder.append(" ").append(city);
        if (country != null && !country.isBlank()) {
            builder.append(" (").append(country).append(")");
        }
        return builder.toString();
    }

    // ------------------------------------------------------------------

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }
}
