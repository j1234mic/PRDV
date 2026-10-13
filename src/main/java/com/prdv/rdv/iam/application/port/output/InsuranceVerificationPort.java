package com.prdv.rdv.iam.application.port.output;

import java.time.LocalDate;

/**
 * Port sortant de verification de l'assurance responsabilite civile
 * professionnelle (RC Pro) des praticiens.
 */
public interface InsuranceVerificationPort {

    InsuranceCheck verify(String company, String policyNumber, LocalDate expiresAt, String practitionerName);

    record InsuranceCheck(boolean valid,
                          String insuranceCompany,
                          String insurancePolicyNumber,
                          LocalDate expiresAt,
                          String reason) {
    }
}
