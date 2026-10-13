package com.prdv.rdv.iam.application.port.output;

/**
 * Port sortant de verification cryptographique d'une assertion biometrique
 * (WebAuthn / FIDO2 / Passkey : empreinte digitale Touch ID, Face ID).
 */
public interface BiometricVerifierPort {

    boolean verifyAssertion(String publicKey, String challengeToken, String credentialId, String signature);
}
