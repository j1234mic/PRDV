package com.prdv.rdv.common.web;

import java.time.Instant;
import java.util.Map;

/**
 * Representation standardisee d'une erreur renvoyee par l'API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        Map<String, String> fieldErrors,
        String path
) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, null, path);
    }

    public static ApiError validation(int status, String message, Map<String, String> fieldErrors, String path) {
        return new ApiError(Instant.now(), status, "VALIDATION_ERROR", message, fieldErrors, path);
    }
}
