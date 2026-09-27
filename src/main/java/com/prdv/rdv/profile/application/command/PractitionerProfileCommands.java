package com.prdv.rdv.profile.application.command;

import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Commandes du cas d'usage « profil praticien / praticienne » (module 2.2).
 *
 * <p>Les blocs structurants (identite, informations medicales, gestion,
 * visibilite) sont transmis sous forme de Value Objects du domaine : ce sont
 * des donnees immuables, validees a la construction, ce qui evite de
 * dupliquer une vingtaine de champs entre la commande et le modele.
 */
public final class PractitionerProfileCommands {

    private PractitionerProfileCommands() {
    }

    public record UpdateIdentity(PractitionerDossier.ProfessionalIdentity identity) {
    }

    public record UpdatePracticeInformation(PractitionerDossier.PracticeInformation information) {
    }

    /**
     * @param iban IBAN professionnel en clair, transmis une seule fois : il est
     *             immediatement tokenise et masque, la valeur brute n'est jamais
     *             persistee. {@code null} conserve les coordonnees bancaires actuelles.
     */
    public record UpdateManagement(PractitionerDossier.ProfessionalManagement management, String iban) {
    }

    public record UpdateVisibility(PractitionerDossier.VisibilityProfile visibility) {
    }

    public record AddNetworkContact(PractitionerDossier.NetworkRole role,
                                    String name,
                                    String specialty,
                                    String registrationNumber,
                                    String city) {
    }

    /** Photo professionnelle ou video de presentation. */
    public record StoreMedia(String originalFilename, String contentType, byte[] content, boolean video) {
    }

    public record OpeningHoursEntry(DayOfWeek day, LocalTime opensAt, LocalTime closesAt,
                                    boolean closed, String note) {
    }

    public record CreateLocation(String name,
                                 String line1,
                                 String line2,
                                 String postalCode,
                                 String city,
                                 String country,
                                 boolean mainLocation,
                                 Double latitude,
                                 Double longitude,
                                 List<OpeningHoursEntry> openingHours,
                                 String phone,
                                 String mobilePhone,
                                 String fax,
                                 String email,
                                 String website,
                                 String virtualTourUrl,
                                 Map<String, String> socialLinks,
                                 boolean wheelchairAccessible,
                                 boolean parkingAvailable,
                                 String publicTransportInfo) {
    }

    public record UpdateLocation(String name,
                                 String line1,
                                 String line2,
                                 String postalCode,
                                 String city,
                                 String country,
                                 Double latitude,
                                 Double longitude,
                                 List<OpeningHoursEntry> openingHours,
                                 String phone,
                                 String mobilePhone,
                                 String fax,
                                 String email,
                                 String website,
                                 String virtualTourUrl,
                                 Map<String, String> socialLinks,
                                 boolean wheelchairAccessible,
                                 boolean parkingAvailable,
                                 String publicTransportInfo) {
    }

    public record AddLocationPhoto(String locationId, PracticeLocation.PhotoType type,
                                   String originalFilename, String contentType, byte[] content,
                                   String caption) {
    }

    public record SubmitRating(int score, String comment) {
    }
}
