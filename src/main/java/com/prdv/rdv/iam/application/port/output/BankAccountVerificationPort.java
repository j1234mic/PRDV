package com.prdv.rdv.iam.application.port.output;

/**
 * Port de verification du RIB/IBAN du praticien pour les reversements
 * (adapteurs : API bancaires PSD2, SPS/Stripe-like). L'IBAN ne transite
 * que sous forme tokenisee dans la persistance.
 */
public interface BankAccountVerificationPort {

    BankCheck verify(String iban, String accountHolderName);

    record BankCheck(boolean valid, String bic, String bankName, String reason) {
    }
}
