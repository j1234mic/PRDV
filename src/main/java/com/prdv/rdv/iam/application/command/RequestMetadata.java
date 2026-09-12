package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.auth.UserSession;

/**
 * Metadonees de la requete transmises par l'adaptateur web aux cas d'usage
 * (IP, User-Agent, geolocalisation) pour les sessions et l'anti-fraude.
 */
public record RequestMetadata(String ipAddress, String userAgent, UserSession.GeoLocation geoLocation) {

    public static RequestMetadata unknown() {
        return new RequestMetadata("unknown", null, UserSession.GeoLocation.unknown());
    }
}
