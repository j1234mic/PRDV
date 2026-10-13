package com.prdv.rdv.iam.domain.model.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * Facteur biometrique enregistre (WebAuthn / FIDO2 / Passkey : empreinte
 * digitale Touch ID, reconnaissance faciale Face ID, cle materielle).
 *
 * <p>Seule la cle publique du terminal est stockee cote serveur : les donnees
 * biometriques brutes ne quittent jamais l'enclave securisee de l'appareil.
 * Le compteur de signatures ({@code signCount}) permet de detecter le clonage
 * d'un authentificateur.
 */
@Getter
@Setter
public class BiometricCredential {

    public enum BiometricModality {
        FINGERPRINT,
        FACE_ID,
        PASSKEY;

        @JsonCreator
        public static BiometricModality from(String value) {
            if (value == null || value.isBlank()) {
                return PASSKEY;
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return switch (normalized) {
                case "FINGERPRINT", "EMPREINTE", "TOUCH_ID", "TOUCHID", "BIOMETRIC_FINGERPRINT" -> FINGERPRINT;
                case "FACE_ID", "FACEID", "VISAGE", "FACE", "BIOMETRIC_FACE" -> FACE_ID;
                case "PASSKEY", "WEBAUTHN", "FIDO2" -> PASSKEY;
                default -> throw new IllegalArgumentException(
                        "Modalite biometrique invalide : \"" + value
                                + "\" (valeurs acceptees : FINGERPRINT, FACE_ID, PASSKEY)");
            };
        }
    }

    private Long id;
    private Long userId;
    private String credentialId;
    private String publicKey;
    private BiometricModality modality;
    private String deviceName;
    private long signCount;
    private boolean revoked;
    private Instant createdAt;
    private Instant lastUsedAt;
    private Instant revokedAt;

    public static BiometricCredential enroll(Long userId, String credentialId, String publicKey,
                                             BiometricModality modality, String deviceName, Clock clock) {
        if (credentialId == null || credentialId.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "L'identifiant du credential biometrique (credentialId) est obligatoire");
        }
        if (publicKey == null || publicKey.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "La cle publique biometrique (publicKey) est obligatoire");
        }
        BiometricCredential c = new BiometricCredential();
        c.userId = userId;
        c.credentialId = credentialId.trim();
        c.publicKey = publicKey.trim();
        c.modality = modality != null ? modality : BiometricModality.FINGERPRINT;
        c.deviceName = (deviceName == null || deviceName.isBlank()) ? "Appareil biometrique" : deviceName.trim();
        c.signCount = 0L;
        c.revoked = false;
        c.createdAt = clock.instant();
        return c;
    }

    /**
     * Valide et enregistre l'utilisation d'une assertion biometrique.
     * Si le terminal fournit un compteur de signatures monotone ({@code newSignCount > 0}),
     * toute valeur inferieure ou egale au compteur precedent signale un clonage potentiel.
     */
    public void recordAssertion(long newSignCount, Clock clock) {
        if (revoked) {
            throw IamException.of(IamErrorCode.TOKEN_INVALID,
                    "Ce facteur biometrique a ete revoque");
        }
        if (newSignCount > 0 && this.signCount > 0 && newSignCount <= this.signCount) {
            throw IamException.of(IamErrorCode.INVALID_CREDENTIALS,
                    "Anomalie de compteur de signature biometrique (suspicion de clonage)");
        }
        if (newSignCount > this.signCount) {
            this.signCount = newSignCount;
        } else {
            this.signCount++;
        }
        this.lastUsedAt = clock.instant();
    }

    public void revoke(Clock clock) {
        this.revoked = true;
        this.revokedAt = clock.instant();
    }
}
