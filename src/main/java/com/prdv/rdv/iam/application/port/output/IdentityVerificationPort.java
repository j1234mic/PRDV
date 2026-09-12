package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.verification.KycDocument;

/**
 * Port KYC : verification d'identite (scan CNI / passeport) via un prestataire
 * (ex : Onfido, Checkout, France Idendification). L'adapteur peut etre
 * asynchrone (webhook) : le port rend une reponse instantanee ou PENDING.
 */
public interface IdentityVerificationPort {

    IdentityCheck verify(KycDocument identityDocument);

    record IdentityCheck(boolean verified, String provider, String reference, String reason) {
    }
}
