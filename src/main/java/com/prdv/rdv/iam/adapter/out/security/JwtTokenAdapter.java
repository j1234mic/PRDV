package com.prdv.rdv.iam.adapter.out.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.prdv.rdv.iam.application.port.output.JwtTokenPort;
import com.prdv.rdv.iam.config.IamProperties;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Production et verification de JWT HMAC-SHA256 avec Nimbus JOSE+JWT.
 * Access token de courte duree (15 min) ; un jeton « challenge » sans
 * permission porte l'authentification en deux etapes (MFA / OTP).
 */
@Component
public class JwtTokenAdapter implements JwtTokenPort {

    private static final String AUTHORITIES_CLAIM = "auth";
    private static final String TOKEN_VERSION_CLAIM = "tv";
    private static final String PURPOSE_CLAIM = "purpose";

    private final byte[] secret;
    private final JWSSigner signer;
    private final IamProperties properties;

    public JwtTokenAdapter(IamProperties properties) {
        this.properties = properties;
        this.secret = properties.getSecurity().getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("La cle JWT doit contenir au moins 32 octets");
        }
        try {
            this.signer = new MACSigner(secret);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser le signer JWT", e);
        }
    }

    @Override
    public String createAccessToken(Long userId, String email, Collection<String> authorities, long tokenVersion) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getSecurity().getJwt().getAccessTokenTtlSeconds());
        JWTClaimsSet claims = baseClaims(userId, now, expiry)
                .claim("email", email)
                .claim(AUTHORITIES_CLAIM, List.copyOf(authorities))
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .build();
        return sign(claims);
    }

    @Override
    public String createChallengeToken(Long userId, String purpose) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getSecurity().getJwt().getChallengeTokenTtlSeconds());
        JWTClaimsSet claims = baseClaims(userId, now, expiry)
                .claim(PURPOSE_CLAIM, purpose)
                .build();
        return sign(claims);
    }

    @Override
    public AccessTokenClaims parseAccessToken(String token) {
        try {
            JWTClaimsSet claims = parseAndVerify(token);
            if (claims.getClaim(PURPOSE_CLAIM) != null) {
                throw IamException.of(IamErrorCode.TOKEN_INVALID, "Jeton de defi inutilisable en acces");
            }
            Long version = claims.getLongClaim(TOKEN_VERSION_CLAIM);
            return new AccessTokenClaims(Long.valueOf(claims.getSubject()),
                    (String) claims.getClaim("email"),
                    version == null ? 0 : version,
                    claims.getExpirationTime().toInstant());
        } catch (IamException e) {
            throw e;
        } catch (Exception e) {
            throw IamException.of(IamErrorCode.TOKEN_INVALID, "Jeton d'acces invalide");
        }
    }

    @Override
    public ChallengeClaims parseChallengeToken(String token, String expectedPurpose) {
        try {
            JWTClaimsSet claims = parseAndVerify(token);
            Object purpose = claims.getClaim(PURPOSE_CLAIM);
            if (purpose == null || !expectedPurpose.equals(purpose)) {
                throw IamException.of(IamErrorCode.TOKEN_INVALID, "Jeton de defi invalide");
            }
            return new ChallengeClaims(Long.valueOf(claims.getSubject()), expectedPurpose,
                    claims.getExpirationTime().toInstant());
        } catch (IamException e) {
            throw e;
        } catch (Exception e) {
            throw IamException.of(IamErrorCode.TOKEN_INVALID, "Jeton de defi invalide");
        }
    }

    @Override
    public long getAccessTokenTtlSeconds() {
        return properties.getSecurity().getJwt().getAccessTokenTtlSeconds();
    }

    private JWTClaimsSet.Builder baseClaims(Long userId, Instant now, Instant expiry) {
        return new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .issuer("prdv-rdv")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry));
    }

    private String sign(JWTClaimsSet claims) {
        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Echec de signature JWT", e);
        }
    }

    private JWTClaimsSet parseAndVerify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secret);
            if (!jwt.verify(verifier)) {
                throw IamException.of(IamErrorCode.TOKEN_INVALID, "Signature du jeton invalide");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime() != null && claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw IamException.of(IamErrorCode.TOKEN_EXPIRED, "Jeton expire");
            }
            return claims;
        } catch (IamException e) {
            throw e;
        } catch (Exception e) {
            throw IamException.of(IamErrorCode.TOKEN_INVALID, "Jeton illisible ou invalide");
        }
    }
}
