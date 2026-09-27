package com.prdv.rdv.profile.adapter.in.web.dto;

import com.prdv.rdv.profile.application.command.PatientIdentityCommands;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO d'entree du profil patient.
 *
 * <p>Les reponses exposent directement les vues applicatives
 * ({@code ProfileViews.*}), comme dans le module 1 : le web ne reformate pas
 * le metier, il traduit seulement les requetes HTTP en commandes.
 */
public final class PatientIdentityDtos {

    private PatientIdentityDtos() {
    }

    public record CivilStatusRequest(
            PatientIdentity.Civility civility,
            @NotBlank @Size(max = 100) String firstName,
            @Size(max = 100) String birthName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 100) String preferredName,
            LocalDate birthDate,
            @Size(max = 200) String birthPlace,
            @Size(max = 100) String birthCountry,
            PatientIdentity.Gender gender,
            PatientIdentity.MaritalStatus maritalStatus,
            @Size(max = 100) String nationality) {

        public PatientIdentityCommands.UpdateCivilStatus toCommand() {
            return new PatientIdentityCommands.UpdateCivilStatus(civility, firstName, birthName, lastName,
                    preferredName, birthDate, birthPlace, birthCountry, gender, maritalStatus, nationality);
        }
    }

    public record ContactEntryRequest(@NotNull PatientIdentity.ContactType type,
                                      @NotBlank @Size(max = 255) String value,
                                      boolean verified,
                                      boolean preferred) {

        public PatientIdentityCommands.ContactEntry toEntry() {
            return new PatientIdentityCommands.ContactEntry(type, value, verified, preferred);
        }
    }

    public record ContactsRequest(@NotEmpty List<@Valid ContactEntryRequest> contacts) {

        public PatientIdentityCommands.ReplaceContacts toCommand() {
            return new PatientIdentityCommands.ReplaceContacts(
                    contacts.stream().map(ContactEntryRequest::toEntry).toList());
        }
    }

    public record AddressEntryRequest(@NotNull PatientIdentity.AddressType type,
                                      @NotBlank @Size(max = 255) String line1,
                                      @Size(max = 255) String line2,
                                      @Size(max = 10) String postalCode,
                                      @NotBlank @Size(max = 120) String city,
                                      @Size(max = 100) String country,
                                      Double latitude,
                                      Double longitude,
                                      boolean isDefault) {

        public PatientIdentityCommands.AddressEntry toEntry() {
            return new PatientIdentityCommands.AddressEntry(type, line1, line2, postalCode, city, country,
                    latitude, longitude, isDefault);
        }
    }

    public record AddressesRequest(@NotEmpty List<@Valid AddressEntryRequest> addresses) {

        public PatientIdentityCommands.ReplaceAddresses toCommand() {
            return new PatientIdentityCommands.ReplaceAddresses(
                    addresses.stream().map(AddressEntryRequest::toEntry).toList());
        }
    }

    public record EmergencyContactRequest(@NotBlank @Size(max = 100) String firstName,
                                          @NotBlank @Size(max = 100) String lastName,
                                          @NotBlank @Size(max = 100) String relationship,
                                          @Size(max = 50) String phone,
                                          @Size(max = 255) String email) {

        public PatientIdentityCommands.UpdateEmergencyContact toCommand() {
            return new PatientIdentityCommands.UpdateEmergencyContact(firstName, lastName, relationship,
                    phone, email);
        }
    }

    public record TreatingPhysicianRequest(@Size(max = 100) String firstName,
                                           @NotBlank @Size(max = 100) String lastName,
                                           @Size(max = 30) String rppsNumber,
                                           @Size(max = 50) String phone,
                                           @Size(max = 255) String email,
                                           boolean declaredToInsurance) {

        public PatientIdentityCommands.DeclareTreatingPhysician toCommand() {
            return new PatientIdentityCommands.DeclareTreatingPhysician(firstName, lastName, rppsNumber,
                    phone, email, declaredToInsurance);
        }
    }

    /** Le NIR est transmis une fois, puis remplace par un token et un masque. */
    public record SocialSecurityNumberRequest(@NotBlank String value) {

        public PatientIdentityCommands.RegisterSocialSecurityNumber toCommand() {
            return new PatientIdentityCommands.RegisterSocialSecurityNumber(value);
        }
    }

    public record InsuranceRequest(@NotNull PatientIdentity.InsuranceType type,
                                   @NotBlank @Size(max = 200) String organization,
                                   @Size(max = 100) String memberNumber,
                                   @Size(max = 100) String contractReference,
                                   LocalDate validUntil) {

        public PatientIdentityCommands.UpdateInsurance toCommand() {
            return new PatientIdentityCommands.UpdateInsurance(type, organization, memberNumber,
                    contractReference, validUntil);
        }
    }

    /** Lecture NFC, scan ou saisie manuelle de la carte Vitale. */
    public record VitaleCardRequest(@NotBlank String nir,
                                    @Size(max = 30) String cardVersion,
                                    LocalDate issuedOn,
                                    LocalDate expiresOn,
                                    @NotNull PatientIdentity.VitaleReadMode readMode) {

        public PatientIdentityCommands.RegisterVitaleCard toCommand(MultipartFile scan) {
            return new PatientIdentityCommands.RegisterVitaleCard(nir, cardVersion, issuedOn, expiresOn,
                    readMode, filename(scan), contentType(scan), content(scan));
        }
    }

    public record DmpLinkRequest(@NotBlank @Size(max = 100) String dmpIdentifier, boolean sharingEnabled) {

        public PatientIdentityCommands.LinkDmp toCommand() {
            return new PatientIdentityCommands.LinkDmp(dmpIdentifier, sharingEnabled);
        }
    }

    public record DmpSharingRequest(boolean sharingEnabled) {

        public PatientIdentityCommands.UpdateDmpSharing toCommand() {
            return new PatientIdentityCommands.UpdateDmpSharing(sharingEnabled);
        }
    }

    // ------------------------------------------------------------------
    // Support multipart
    // ------------------------------------------------------------------

    public static PatientIdentityCommands.StoreFile toStoreFile(MultipartFile file) {
        return new PatientIdentityCommands.StoreFile(filename(file), contentType(file), content(file));
    }

    public static String filename(MultipartFile file) {
        return file == null ? null : file.getOriginalFilename();
    }

    public static String contentType(MultipartFile file) {
        return file == null ? null : file.getContentType();
    }

    public static byte[] content(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Lecture du fichier televerse impossible", exception);
        }
    }
}
