package com.prdv.rdv.iam.application.port.output;

/**
 * Port TOTP (RFC 6238) : generation du secret, URI de provisionnement
 * (QR code Google Authenticator) et verification du code a 6 chiffres.
 */
public interface TotpPort {

    String generateSecret();

    String provisioningUri(String secret, String account);

    boolean verifyCode(String secret, String code);
}
