package com.prdv.rdv.profile.domain.model.practitioner;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.common.ContactFormats;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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

        /**
         * Identifiant stable et opaque de la photo, derive de sa cle de stockage
         * (UUID de type 3) : il survit au rechargement et ne contient aucun
         * separateur de chemin, il peut donc figurer dans une URL.
         */
        public String photoId() {
            return UUID.nameUUIDFromBytes(storageKey.getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private static final int MAX_PHOTOS = 20;
    private static final int MAX_SOCIAL_LINKS = 10;
    private static final Pattern SOCIAL_PLATFORM = Pattern.compile("^[a-z0-9_-]{2,30}$");

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

    /** Geolocalisation : latitude et longitude sont fournies ensemble, bornees, et jamais NaN. */
    public void geolocate(Double newLatitude, Double newLongitude, Clock clock) {
        if ((newLatitude == null) != (newLongitude == null)) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                    "La latitude et la longitude doivent etre renseignees ensemble");
        }
        if (newLatitude != null && !(newLatitude >= -90 && newLatitude <= 90)) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID, "Latitude invalide : " + newLatitude);
        }
        if (newLongitude != null && !(newLongitude >= -180 && newLongitude <= 180)) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID, "Longitude invalide : " + newLongitude);
        }
        this.latitude = newLatitude;
        this.longitude = newLongitude;
        touch(clock);
    }

    /** Coordonnees professionnelles : chaque valeur renseignee doit avoir un format valide. */
    public void updateCoordinates(String newPhone, String newMobilePhone, String newFax, String newEmail,
                                  String newWebsite, String newVirtualTourUrl, Clock clock) {
        String checkedPhone = ContactFormats.phone(newPhone, ProfileErrorCode.LOCATION_INVALID,
                "Telephone fixe");
        String checkedMobile = ContactFormats.phone(newMobilePhone, ProfileErrorCode.LOCATION_INVALID,
                "Telephone mobile");
        String checkedFax = ContactFormats.phone(newFax, ProfileErrorCode.LOCATION_INVALID, "Fax");
        String checkedEmail = ContactFormats.email(newEmail, ProfileErrorCode.LOCATION_INVALID,
                "Email professionnel");
        String checkedWebsite = ContactFormats.httpUrl(newWebsite, ProfileErrorCode.LOCATION_INVALID,
                "Site web");
        String checkedTour = ContactFormats.httpUrl(newVirtualTourUrl, ProfileErrorCode.LOCATION_INVALID,
                "Visite virtuelle 360");
        this.phone = checkedPhone;
        this.mobilePhone = checkedMobile;
        this.fax = checkedFax;
        this.email = checkedEmail;
        this.website = checkedWebsite;
        this.virtualTourUrl = checkedTour;
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

    public Optional<Photo> findPhoto(String photoId) {
        return photos.stream().filter(photo -> photo.photoId().equals(photoId)).findFirst();
    }

    /** Retire la photo identifiee et la renvoie, pour supprimer son fichier apres sauvegarde. */
    public Photo removePhotoById(String photoId, Clock clock) {
        Photo target = findPhoto(photoId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                        "Aucune photo ne correspond a l'identifiant " + photoId));
        this.photos.remove(target);
        touch(clock);
        return target;
    }

    /** Cles de stockage des photos : a supprimer lors de l'effacement du lieu ou du compte. */
    public List<String> mediaStorageKeys() {
        return photos.stream().map(Photo::storageKey).toList();
    }

    /**
     * Remplace l'ensemble des reseaux sociaux du lieu. Une carte vide retire
     * tous les liens ; un lien se retire en l'omettant de la carte envoyee.
     */
    public void replaceSocialLinks(Map<String, String> links, Clock clock) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        if (links != null) {
            for (Map.Entry<String, String> entry : links.entrySet()) {
                String platform = entry.getKey() == null
                        ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
                if (platform.isEmpty() && (entry.getValue() == null || entry.getValue().isBlank())) {
                    continue;
                }
                if (!SOCIAL_PLATFORM.matcher(platform).matches()) {
                    throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                            "Plateforme de reseau social invalide : " + entry.getKey());
                }
                String url = ContactFormats.httpUrl(entry.getValue(), ProfileErrorCode.LOCATION_INVALID,
                        "Lien " + platform);
                if (url == null) {
                    throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                            "Un lien de reseau social exige une URL : " + platform);
                }
                cleaned.put(platform, url);
            }
        }
        if (cleaned.size() > MAX_SOCIAL_LINKS) {
            throw ProfileException.of(ProfileErrorCode.LOCATION_INVALID,
                    "Trop de reseaux sociaux (maximum " + MAX_SOCIAL_LINKS + ")");
        }
        this.socialLinks = cleaned;
        touch(clock);
    }

    /** Ce lieu devient le lieu principal (le retrait des autres lieux est du ressort du service). */
    public void promoteToMain(Clock clock) {
        this.mainLocation = true;
        touch(clock);
    }

    public void demoteFromMain(Clock clock) {
        this.mainLocation = false;
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
