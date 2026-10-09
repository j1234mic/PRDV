package com.prdv.rdv.profile.domain.model.practitioner;

import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lieu d'exercice : reseaux sociaux, geolocalisation, coordonnees et photos. */
class PracticeLocationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Les reseaux sociaux sont remplaces : un lien omis est retire, une carte vide efface tout")
    void socialLinksAreReplacedAndEmptyMapClearsThem() {
        PracticeLocation location = location();

        location.replaceSocialLinks(Map.of(
                "linkedin", "https://www.linkedin.com/in/dr-martin",
                "instagram", "https://instagram.com/cabinet"), CLOCK);
        assertThat(location.getSocialLinks()).containsOnlyKeys("linkedin", "instagram");

        location.replaceSocialLinks(Map.of("linkedin", "https://www.linkedin.com/in/dr-martin"), CLOCK);
        assertThat(location.getSocialLinks()).containsOnlyKeys("linkedin");

        location.replaceSocialLinks(Map.of(), CLOCK);
        assertThat(location.getSocialLinks()).isEmpty();
    }

    @Test
    @DisplayName("La plateforme et l'URL d'un reseau social sont verifiees")
    void socialPlatformAndUrlAreValidated() {
        PracticeLocation location = location();

        assertThatThrownBy(() -> location.replaceSocialLinks(Map.of("Bad Platform!", "https://example.fr"), CLOCK))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Plateforme");
        assertThatThrownBy(() -> location.replaceSocialLinks(Map.of("linkedin", "javascript:alert(1)"), CLOCK))
                .isInstanceOf(ProfileException.class);
    }

    @Test
    @DisplayName("La geolocalisation exige latitude et longitude ensemble, bornees et non NaN")
    void geolocationNeedsBothCoordinatesWithinBounds() {
        PracticeLocation location = location();

        assertThatThrownBy(() -> location.geolocate(48.85, null, CLOCK)).isInstanceOf(ProfileException.class);
        assertThatThrownBy(() -> location.geolocate(Double.NaN, 2.35, CLOCK)).isInstanceOf(ProfileException.class);
        assertThatThrownBy(() -> location.geolocate(91.0, 2.35, CLOCK)).isInstanceOf(ProfileException.class);

        location.geolocate(48.85, 2.35, CLOCK);
        assertThat(location.getLatitude()).isEqualTo(48.85);
        assertThat(location.getLongitude()).isEqualTo(2.35);
    }

    @Test
    @DisplayName("Les coordonnees professionnelles sont normalisees et verifiees")
    void contactCoordinatesAreFormatChecked() {
        PracticeLocation location = location();

        location.updateCoordinates("01 23 45 67 89", null, null, "Accueil@Cabinet.fr",
                "https://cabinet.example.fr", null, CLOCK);
        assertThat(location.getEmail()).isEqualTo("accueil@cabinet.fr");
        assertThat(location.getWebsite()).isEqualTo("https://cabinet.example.fr");

        assertThatThrownBy(() -> location.updateCoordinates("pas un numero", null, null, null, null, null, CLOCK))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("Telephone fixe");
    }

    @Test
    @DisplayName("Retirer une photo renvoie cette photo et conserve les autres ; l'identifiant est stable")
    void removingAPhotoReturnsItAndKeepsTheOthers() {
        PracticeLocation location = location();
        PracticeLocation.Photo facade = new PracticeLocation.Photo(
                "practice-locations/42/photos/a-facade.jpg", PracticeLocation.PhotoType.EXTERIOR, "Facade");
        PracticeLocation.Photo waitingRoom = new PracticeLocation.Photo(
                "practice-locations/42/photos/b-salle.jpg", PracticeLocation.PhotoType.WAITING_ROOM, null);
        location.addPhoto(facade, CLOCK);
        location.addPhoto(waitingRoom, CLOCK);

        String waitingRoomId = waitingRoom.photoId();
        assertThat(waitingRoomId).doesNotContain("/");
        assertThat(new PracticeLocation.Photo(waitingRoom.storageKey(), PracticeLocation.PhotoType.RECEPTION,
                "autre legende").photoId()).isEqualTo(waitingRoomId);

        PracticeLocation.Photo removed = location.removePhotoById(waitingRoomId, CLOCK);

        assertThat(removed).isEqualTo(waitingRoom);
        assertThat(location.mediaStorageKeys()).containsExactly(facade.storageKey());
        assertThatThrownBy(() -> location.removePhotoById("inconnu", CLOCK)).isInstanceOf(ProfileException.class);
    }

    private static PracticeLocation location() {
        return PracticeLocation.create(42L, "Cabinet Centre", "12 rue de la Paix", "75002", "Paris", "FR",
                true, CLOCK);
    }
}
