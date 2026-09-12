package com.prdv.rdv.iam.adapter.in.web.oauth;

import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.port.input.SocialAuthenticationUseCase;
import com.prdv.rdv.iam.application.port.output.GeoLocationPort;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.config.IamProperties;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Apres une connexion OAuth2/OIDC reussie, cree/lie le compte utilisateur,
 * emet les jetons PRDV puis redirige vers le frontend avec les jetons en
 * fragment d'URL (ils ne transitent jamais par les logs serveur).
 */
@Component
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialAuthenticationUseCase socialAuthenticationUseCase;
    private final GeoLocationPort geoLocationPort;
    private final IamProperties properties;

    public SocialLoginSuccessHandler(SocialAuthenticationUseCase socialAuthenticationUseCase,
                                     GeoLocationPort geoLocationPort, IamProperties properties) {
        this.socialAuthenticationUseCase = socialAuthenticationUseCase;
        this.geoLocationPort = geoLocationPort;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification sociale invalide");
            return;
        }
        OAuth2User user = oauthToken.getPrincipal();
        Map<String, Object> attributes = user.getAttributes();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        SocialAccount.Provider provider = mapProvider(registrationId);
        String providerUserId = stringAttribute(attributes, "sub", "id");
        String email = stringAttribute(attributes, "email");
        String firstName = stringAttribute(attributes, "given_name", "first_name");
        String lastName = stringAttribute(attributes, "family_name", "last_name");
        if (user instanceof OidcUser oidc && oidc.getSubject() != null) {
            providerUserId = oidc.getSubject();
        }

        RequestMetadata metadata = new RequestMetadata(clientIp(request), request.getHeader("User-Agent"),
                geoLocationPort.locate(clientIp(request)));

        AuthResults.TokenSet tokens = socialAuthenticationUseCase.authenticate(
                provider, providerUserId, email, firstName, lastName, metadata);

        String redirect = properties.getFrontend().getOauthRedirectUrl();
        response.sendRedirect(redirect
                + "#access_token=" + encode(tokens.accessToken())
                + "&refresh_token=" + encode(tokens.refreshToken())
                + "&expires_in=" + tokens.expiresInSeconds()
                + "&provider=" + registrationId);
    }

    private SocialAccount.Provider mapProvider(String registrationId) {
        return switch (registrationId == null ? "" : registrationId.toLowerCase()) {
            case "google" -> SocialAccount.Provider.GOOGLE;
            case "facebook" -> SocialAccount.Provider.FACEBOOK;
            case "apple" -> SocialAccount.Provider.APPLE;
            case "microsoft", "azure" -> SocialAccount.Provider.MICROSOFT;
            default -> SocialAccount.Provider.OIDC_ENTERPRISE;
        };
    }

    private String stringAttribute(Map<String, Object> attributes, String... names) {
        for (String name : names) {
            Object value = attributes.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
