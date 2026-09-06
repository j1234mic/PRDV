package com.prdv.adapters.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuration externalisee : le secret vient de l'environnement (Docker secret / vault), jamais du code. */
@ConfigurationProperties(prefix = "prdv.security.jwt")
public class JwtProperties {

    /** Cle HMAC de 32 octets minimum, encodee en Base64. */
    private String secret;
    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(14);
    private String issuer = "prdv-backend";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getAccessTtl() { return accessTtl; }
    public void setAccessTtl(Duration accessTtl) { this.accessTtl = accessTtl; }
    public Duration getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(Duration refreshTtl) { this.refreshTtl = refreshTtl; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}
