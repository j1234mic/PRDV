package com.prdv.rdv.iam.adapter.out.importdata;

import com.prdv.rdv.iam.application.port.output.ProfileImportPort;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Import CSV (couche anti-corruption) d'un profil patient depuis une autre
 * plateforme. Format attendu :
 * <pre>
 * firstName,lastName,birthDate,email,phone,sourcePlatform
 * Marie,Dupont,1990-05-12,marie.dupont@example.com,+33612345678,Doctolib
 * </pre>
 */
@Component
public class CsvProfileImportAdapter implements ProfileImportPort {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public boolean supports(String format) {
        return format != null && format.toLowerCase(Locale.ROOT).contains("csv");
    }

    @Override
    public ImportedProfile parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw IamException.of(IamErrorCode.IMPORT_FORMAT_INVALID, "Contenu CSV vide");
        }
        String[] lines = rawContent.strip().split("\\R");
        if (lines.length < 2) {
            throw IamException.of(IamErrorCode.IMPORT_FORMAT_INVALID,
                    "Le CSV doit contenir une ligne d'en-tete et au moins une ligne de donnees");
        }

        Map<String, String> row = new HashMap<>();
        String[] headers = lines[0].split(",");
        String[] values = lines[1].split(",", -1);
        for (int i = 0; i < headers.length && i < values.length; i++) {
            row.put(headers[i].trim(), values[i].trim());
        }

        LocalDate birthDate = null;
        if (row.containsKey("birthDate") && !row.get("birthDate").isBlank()) {
            try {
                birthDate = LocalDate.parse(row.get("birthDate"), ISO_DATE);
            } catch (RuntimeException e) {
                throw IamException.of(IamErrorCode.IMPORT_FORMAT_INVALID, "Date de naissance invalide");
            }
        }

        return new ImportedProfile(
                row.get("firstName"),
                row.get("lastName"),
                birthDate,
                row.get("email"),
                row.get("phone"),
                row.getOrDefault("sourcePlatform", "csv-import"));
    }
}
