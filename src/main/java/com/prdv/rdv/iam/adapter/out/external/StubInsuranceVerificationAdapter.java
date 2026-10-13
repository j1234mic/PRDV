package com.prdv.rdv.iam.adapter.out.external;

import com.prdv.rdv.iam.application.port.output.InsuranceVerificationPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Verification d'une assurance Responsabilite Civile Professionnelle (RC Pro)
 * medicale (ex. MACSF, Relyens, Branchet, Medicale de France).
 */
@Component
public class StubInsuranceVerificationAdapter implements InsuranceVerificationPort {

    private static final Pattern POLICY_PATTERN = Pattern.compile("^[A-Za-z0-9\\-/]{6,40}$");

    private final Clock clock;

    public StubInsuranceVerificationAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public InsuranceVerification verify(String insuranceCompany,
                                        String policyNumber,
                                        LocalDate expiresAt,
                                        String rppsOrAdeli) {
        if (insuranceCompany == null || insuranceCompany.isBlank()) {
            return new InsuranceVerification(false, null, policyNumber, expiresAt,
                    "Compagnie d'assurance RC Pro manquante");
        }
        if (policyNumber == null || !POLICY_PATTERN.matcher(policyNumber.trim()).matches()) {
            return new InsuranceVerification(false, insuranceCompany.trim(), policyNumber, expiresAt,
                    "Numero de police d'assurance RC Pro invalide (6 a 40 caracteres alphanumeriques)");
        }
        LocalDate today = LocalDate.now(clock);
        if (expiresAt != null && expiresAt.isBefore(today)) {
            return new InsuranceVerification(false, insuranceCompany.trim(), policyNumber.trim(), expiresAt,
                    "Attestation d'assurance RC Pro expiree le " + expiresAt);
        }
        LocalDate effectiveExpiry = expiresAt != null ? expiresAt : today.plusYears(1);
        return new InsuranceVerification(
                true,
                insuranceCompany.trim(),
                policyNumber.trim().toUpperCase(),
                effectiveExpiry,
                "Assurance RC Pro validee (" + insuranceCompany.trim() + ")");
    }
}
