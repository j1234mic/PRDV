package com.prdv.rdv.profile.application.command;

import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;

import java.time.LocalDate;
import java.util.List;

/**
 * Commandes du cas d'usage « identite patient » (module 2.1).
 *
 * <p>Les commandes sont des donnees immuables : elles ne contiennent aucune
 * annotation web ni aucune dependance servlet (le contenu binaire est
 * transporte sous forme de {@code byte[]}).
 */
public final class PatientIdentityCommands {

    private PatientIdentityCommands() {
    }

    public record UpdateCivilStatus(PatientIdentity.Civility civility,
                                    String firstName,
                                    String birthName,
                                    String lastName,
                                    String preferredName,
                                    LocalDate birthDate,
                                    String birthPlace,
                                    String birthCountry,
                                    PatientIdentity.Gender gender,
                                    PatientIdentity.MaritalStatus maritalStatus,
                                    String nationality) {
    }

    public record ContactEntry(PatientIdentity.ContactType type,
                               String value,
                               boolean verified,
                               boolean preferred) {
    }

    public record ReplaceContacts(List<ContactEntry> contacts) {
    }

    public record AddressEntry(PatientIdentity.AddressType type,
                               String line1,
                               String line2,
                               String postalCode,
                               String city,
                               String country,
                               Double latitude,
                               Double longitude,
                               boolean isDefault) {
    }

    public record ReplaceAddresses(List<AddressEntry> addresses) {
    }

    public record UpdateEmergencyContact(String firstName,
                                         String lastName,
                                         String relationship,
                                         String phone,
                                         String email) {
    }

    public record DeclareTreatingPhysician(String firstName,
                                           String lastName,
                                           String rppsNumber,
                                           String phone,
                                           String email,
                                           boolean declaredToInsurance) {
    }

    /** Le NIR est transmis en clair sur le canal TLS puis immediatement tokenise. */
    public record RegisterSocialSecurityNumber(String value) {
    }

    public record UpdateInsurance(PatientIdentity.InsuranceType type,
                                  String organization,
                                  String memberNumber,
                                  String contractReference,
                                  LocalDate validUntil) {
    }

    /** Fichier binaire (photo de profil ou piece d'identite). */
    public record StoreFile(String originalFilename, String contentType, byte[] content) {
    }

    /** Carte Vitale : lecture NFC (champs structurels) et/ou scan (fichier). */
    public record RegisterVitaleCard(String nir,
                                     String cardVersion,
                                     LocalDate issuedOn,
                                     LocalDate expiresOn,
                                     PatientIdentity.VitaleReadMode readMode,
                                     String scanFilename,
                                     String scanContentType,
                                     byte[] scanContent) {
    }

    public record LinkDmp(String dmpIdentifier, boolean sharingEnabled) {
    }

    public record UpdateDmpSharing(boolean sharingEnabled) {
    }
}
