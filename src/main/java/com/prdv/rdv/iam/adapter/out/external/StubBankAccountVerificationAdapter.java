package com.prdv.rdv.iam.adapter.out.external;

import com.prdv.rdv.iam.application.port.output.BankAccountVerificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verification RIB/IBAN simulee. En production : API de verification de
 * titulaire (PSD2) ou service de type Ponto / CheckRIB.
 * Regle simulee : IBAN de longueur >= 14 caracteres.
 */
@Component
public class StubBankAccountVerificationAdapter implements BankAccountVerificationPort {

    private static final Logger log = LoggerFactory.getLogger(StubBankAccountVerificationAdapter.class);

    @Override
    public BankCheck verify(String iban, String accountHolderName) {
        log.info("[BANK] Verification IBAN pour {}", accountHolderName);
        String normalized = iban == null ? "" : iban.replaceAll("\\s+", "");
        if (normalized.length() < 14) {
            return new BankCheck(false, null, null, "IBAN trop court");
        }
        return new BankCheck(true, "STUGBXXX", "Banque simulee", "coordonnees coherentes");
    }
}
