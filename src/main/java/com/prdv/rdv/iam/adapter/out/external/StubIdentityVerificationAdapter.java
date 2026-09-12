package com.prdv.rdv.iam.adapter.out.external;

import com.prdv.rdv.iam.application.port.output.IdentityVerificationPort;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapteur KYC simule (scan CNI / passeport). En production, brancher un
 * prestataire (Onfido, Checkout.com Identity, France Identite) ; le port
 * resterait identique. Ici, toute piece correctement deposee est validee.
 */
@Component
public class StubIdentityVerificationAdapter implements IdentityVerificationPort {

    private static final Logger log = LoggerFactory.getLogger(StubIdentityVerificationAdapter.class);

    @Override
    public IdentityCheck verify(KycDocument identityDocument) {
        log.info("[KYC] Verification d'identite pour le document {} (type {})",
                identityDocument.getId(), identityDocument.getType());
        return new IdentityCheck(true, "stub-kyc-provider",
                UUID.randomUUID().toString(), "document lisible et non expire");
    }
}
