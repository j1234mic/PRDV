package com.prdv.rdv.iam.adapter.out.external;

import com.prdv.rdv.iam.application.port.output.GeoLocationPort;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import org.springframework.stereotype.Component;

/**
 * Geolocalisation neutre (position inconnue). Brancher ensuite un adapteur
 * MaxMind GeoIP2 ou IPinfo en implementant le meme port.
 */
@Component
public class UnknownGeoLocationAdapter implements GeoLocationPort {

    @Override
    public UserSession.GeoLocation locate(String ipAddress) {
        return UserSession.GeoLocation.unknown();
    }
}
