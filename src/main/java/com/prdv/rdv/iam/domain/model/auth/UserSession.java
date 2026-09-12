package com.prdv.rdv.iam.domain.model.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Session de connexion : historique complet (IP, terminal, geolocalisation),
 * utilisee pour l'ecran « appareils connectes », la detection de connexions
 * suspectes et les logs de connexion.
 */
@Getter
@Setter
public class UserSession {

    /**
     * Geolocalisation derivee de l'IP ; peut etre inconnue
     * (l'adapter de geolocalisation est un port branchable sur MaxMind, IPinfo...).
     */
    public record GeoLocation(String country, String city, Double latitude, Double longitude) {
        public static GeoLocation unknown() {
            return new GeoLocation(null, null, null, null);
        }

        public boolean isKnown() {
            return country != null;
        }
    }

    private Long id;
    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String deviceLabel;
    private GeoLocation geoLocation;
    private boolean suspicious;
    private Instant createdAt;
    private Instant lastSeenAt;
    private Instant revokedAt;

    public static UserSession start(Long userId, String ipAddress, String userAgent,
                                    String deviceLabel, GeoLocation geoLocation,
                                    boolean suspicious, Clock clock) {
        UserSession s = new UserSession();
        s.userId = userId;
        s.ipAddress = ipAddress;
        s.userAgent = userAgent;
        s.deviceLabel = deviceLabel;
        s.geoLocation = geoLocation == null ? GeoLocation.unknown() : geoLocation;
        s.suspicious = suspicious;
        s.createdAt = clock.instant();
        s.lastSeenAt = s.createdAt;
        return s;
    }

    public void touch(Clock clock) {
        this.lastSeenAt = clock.instant();
    }

    public void revoke(Clock clock) {
        this.revokedAt = clock.instant();
    }

    public boolean isActive() {
        return revokedAt == null;
    }
}
