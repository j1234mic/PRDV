package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.user.PatientProfile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Commandes d'inscription multi-profils.
 */
public final class RegistrationCommands {

    private RegistrationCommands() {
    }

    public record RegisterPatient(
            String email,
            String phone,
            String password,
            String firstName,
            String lastName,
            LocalDate birthDate,
            PatientProfile.Gender gender,
            Long guardianUserId,
            RequestMetadata metadata) {
    }

    public record RegisterPractitioner(
            String email,
            String phone,
            String password,
            String firstName,
            String lastName,
            String specialty,
            String rppsNumber,
            String adeliNumber,
            String iban,
            RequestMetadata metadata) {
    }

    public record RegisterSecretary(
            String email,
            String firstName,
            String lastName,
            Set<Long> supervisedPractitionerIds,
            Set<String> permissionCodes) {
    }

    public record RegisterEstablishment(
            String email,
            String phone,
            String password,
            String legalName,
            String siret,
            String address,
            List<String> departments,
            RequestMetadata metadata) {
    }
}
