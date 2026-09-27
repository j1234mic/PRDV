package com.prdv.rdv.profile.application.port.output;

import java.util.Optional;

/**
 * Port de sortie : geocodage d'une adresse de cabinet (Google Geocoding,
 * BAN/Base Adresse Nationale, Nominatim...). L'adapteur fourni ne resout
 * aucune adresse et laisse la geolocalisation fournie par le praticien.
 */
public interface AddressGeocodingPort {

    Optional<GeoPoint> geocode(String line1, String postalCode, String city, String country);

    record GeoPoint(double latitude, double longitude, String provider) {
    }
}
