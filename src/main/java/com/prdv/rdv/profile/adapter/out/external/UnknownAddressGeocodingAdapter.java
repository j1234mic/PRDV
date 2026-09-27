package com.prdv.rdv.profile.adapter.out.external;

import com.prdv.rdv.profile.application.port.output.AddressGeocodingPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Geocodage neutre : ne resout aucune adresse.
 *
 * <p>La geolocalisation reste celle fournie par le praticien (ou par le
 * client web). Brancher la Base Adresse Nationale (api-adresse.data.gouv.fr),
 * Google Geocoding ou Nominatim sur {@link AddressGeocodingPort} complete
 * automatiquement les adresses saisies, sans modification des cas d'usage.
 */
@Component
public class UnknownAddressGeocodingAdapter implements AddressGeocodingPort {

    @Override
    public Optional<GeoPoint> geocode(String line1, String postalCode, String city, String country) {
        return Optional.empty();
    }
}
