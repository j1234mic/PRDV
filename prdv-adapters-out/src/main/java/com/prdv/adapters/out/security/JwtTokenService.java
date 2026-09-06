package com.prdv.adapters.out.security;

import com.prdv.identity.application.model.AuthTokens;
import com.prdv.identity.application.port.out.TokenIssuer;
import com.prdv.identity.application.port.out.TokenVerifier;
import com.prdv.identity.domain.model.TokenType;
import com.prdv.identity.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * ADAPTATEUR du port TokenIssuer/TokenVerifier (jjwt, HS256).
 * Grace a l'inversion de dependance, le coeur ignore la bibliotheque : changer
 * de format (RS256 + JWKS, PASETO...) ne touche AUCUNE ligne du domaine.
 */
@Component
public class JwtTokenService implements TokenIssuer, TokenVerifier {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtTokenService(JwtProperties props) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(props.getSecret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("prdv.security.jwt.secret doit etre du Base64 (32 octets min)", e);
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("prdv.security.jwt.secret trop court (32 octets minimum pour HS256)");
        }
        this.key = Keys.hmacShaKeyFor(decoded);
        this.props = props;
    }

    @Override
    public AuthTokens issue(User user) {
        Instant now = Instant.now();
        String access = build(user, now, TokenType.ACCESS, now.plus(props.getAccessTtl()));
        String refresh = build(user, now, TokenType.REFRESH, now.plus(props.getRefreshTtl()));
        return new AuthTokens(access, refresh, "Bearer", props.getAccessTtl().toSeconds());
    }

    private String build(User user, Instant now, TokenType type, Instant expiry) {
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_ROLE, user.role().name())
                .claim(CLAIM_TYPE, type.name())
                .issuer(props.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    @Override
    public Optional<Payload> verify(String token, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            TokenType actual = TokenType.valueOf(claims.get(CLAIM_TYPE, String.class));
            if (actual != expectedType) {
                return Optional.empty(); // un refresh token n'est pas un access token
            }
            return Optional.of(new Payload(Long.parseLong(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    claims.get(CLAIM_ROLE, String.class), actual));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
