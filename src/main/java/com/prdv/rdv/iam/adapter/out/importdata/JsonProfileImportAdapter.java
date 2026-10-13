package com.prdv.rdv.iam.adapter.out.importdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdv.rdv.iam.application.port.output.ProfileImportPort;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

/**
 * Import JSON / FHIR / plateformes tierces (Doctolib, Maiia, Keldoc, etc.)
 * d'un profil patient (couche anti-corruption).
 */
@Component
public class JsonProfileImportAdapter implements ProfileImportPort {

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "json", "application/json", "doctolib", "maiia", "keldoc", "fhir");

    private final ObjectMapper objectMapper;

    public JsonProfileImportAdapter() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(String format) {
        if (format == null || format.isBlank()) {
            return false;
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_FORMATS.contains(normalized) || normalized.contains("json");
    }

    @Override
    public ImportedProfile parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw IamException.of(IamErrorCode.IMPORT_FORMAT_INVALID, "Contenu JSON vide");
        }
        try {
            JsonNode root = objectMapper.readTree(rawContent);
            String firstName = textOrNull(root, "firstName", "givenName", "prenom");
            String lastName = textOrNull(root, "lastName", "familyName", "nom");
            String birthRaw = textOrNull(root, "birthDate", "dateOfBirth", "dateNaissance");
            LocalDate birthDate = birthRaw != null && !birthRaw.isBlank()
                    ? LocalDate.parse(birthRaw.trim()) : null;
            String email = textOrNull(root, "email", "mail");
            String phone = textOrNull(root, "phone", "phoneNumber", "telephone");
            String sourcePlatform = textOrNull(root, "sourcePlatform", "platform", "source");
            if (sourcePlatform == null || sourcePlatform.isBlank()) {
                sourcePlatform = "json-import";
            }
            return new ImportedProfile(firstName, lastName, birthDate, email, phone, sourcePlatform);
        } catch (IamException e) {
            throw e;
        } catch (Exception e) {
            throw IamException.of(IamErrorCode.IMPORT_FORMAT_INVALID,
                    "Contenu JSON d'import invalide : " + e.getMessage());
        }
    }

    private static String textOrNull(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode child = node.get(name);
            if (child != null && !child.isNull() && !child.asText().isBlank()) {
                return child.asText().trim();
            }
        }
        return null;
    }
}
