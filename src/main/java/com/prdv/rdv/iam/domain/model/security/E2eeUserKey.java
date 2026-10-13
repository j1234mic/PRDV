package com.prdv.rdv.iam.domain.model.security;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Cle publique utilisateur pour le chiffrement de bout en bout (E2EE) des
 * donnees medicales sensibles (X25519 / ECDH P-256 / RSA-OAEP).
 *
 * <p>Permet l'echange d'enveloppes chiffrees de bout en bout entre patient et
 * praticien : la cle privee reste sur le terminal de l'utilisateur tandis que
 * l'annuaire de cles publiques authentifiees est porte par le module IAM.
 */
@Getter
@Setter
public class E2eeUserKey {

    private Long id;
    private Long userId;
    private String keyId;
    private String algorithm;
    private String publicKeyPem;
    private String fingerprint;
    private boolean active;
    private Instant createdAt;
    private Instant rotatedAt;

    public static E2eeUserKey register(Long userId, String keyId, String algorithm,
                                       String publicKeyPem, String fingerprint, Clock clock) {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "La cle publique E2EE est obligatoire");
        }
        E2eeUserKey key = new E2eeUserKey();
        key.userId = userId;
        key.keyId = keyId;
        key.algorithm = (algorithm == null || algorithm.isBlank()) ? "ECDH-P256-AES256GCM" : algorithm.trim();
        key.publicKeyPem = publicKeyPem.trim();
        key.fingerprint = fingerprint;
        key.active = true;
        key.createdAt = clock.instant();
        return key;
    }

    public void deactivate(Clock clock) {
        this.active = false;
        this.rotatedAt = clock.instant();
    }
}
