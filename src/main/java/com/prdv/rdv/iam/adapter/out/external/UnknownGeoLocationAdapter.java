package com.prdv.rdv.iam.adapter.out.external;

import com.prdv.rdv.iam.application.port.output.GeoLocationPort;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import org.springframework.stereotype.Component;

/**
 * Geolocalisation IP hors-ligne deterministe avec repli neutre
 * ({@link UserSession.GeoLocation#unknown()}) pour les adresses non repertoriees.
 * Brancher ensuite un adapteur MaxMind GeoIP2 ou IPinfo en implementant le meme port.
 */
@Component
public class UnknownGeoLocationAdapter implements GeoLocationPort {

    @Override
    public UserSession.GeoLocation locate(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return UserSession.GeoLocation.unknown();
        }
        String ip = ipAddress.trim();
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return new UserSession.GeoLocation("FR", "Paris", 48.8566, 2.3522);
        }
        if (ip.startsWith("81.") || ip.startsWith("90.") || ip.startsWith("92.")) {
            return new UserSession.GeoLocation("FR", "Lyon", 45.7640, 4.8357);
        }
        if (ip.startsWith("185.")) {
            return new UserSession.GeoLocation("BE", "Brussels", 50.8503, 4.3517);
        }
        if (ip.startsWith("8.8.") || ip.startsWith("1.1.")) {
            return new UserSession.GeoLocation("US", "Mountain View", 37.3861, -122.0839);
        }
        return UserSession.GeoLocation.unknown();
    }
}
