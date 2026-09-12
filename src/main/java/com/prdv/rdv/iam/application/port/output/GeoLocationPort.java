package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.UserSession;

/**
 * Geolocalisation d'une adresse IP pour les journaux de connexion et
 * l'anti-fraude (adapteurs : MaxMind GeoIP2, IPinfo). L'adapteur par defaut
 * retourne une position inconnue.
 */
public interface GeoLocationPort {

    UserSession.GeoLocation locate(String ipAddress);
}
