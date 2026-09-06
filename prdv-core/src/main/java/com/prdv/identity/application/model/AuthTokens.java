package com.prdv.identity.application.model;

/** Paire de jetons emise a la connexion / au refresh. */
public record AuthTokens(String accessToken, String refreshToken, String tokenType, long accessExpiresInSeconds) {
}
