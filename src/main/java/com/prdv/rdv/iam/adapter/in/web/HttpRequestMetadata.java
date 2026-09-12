package com.prdv.rdv.iam.adapter.in.web;

import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.port.output.GeoLocationPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Extrait les metadonnees cliente (IP derriere proxy, User-Agent,
 * geolocalisation) pour les cas d'usage de connexion.
 */
@Component
public class HttpRequestMetadata {

    private final GeoLocationPort geoLocationPort;

    public HttpRequestMetadata(GeoLocationPort geoLocationPort) {
        this.geoLocationPort = geoLocationPort;
    }

    public RequestMetadata from(HttpServletRequest request) {
        String ip = clientIp(request);
        return new RequestMetadata(ip, request.getHeader("User-Agent"), geoLocationPort.locate(ip));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp : request.getRemoteAddr();
    }
}
