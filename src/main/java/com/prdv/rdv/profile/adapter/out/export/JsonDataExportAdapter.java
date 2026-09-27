package com.prdv.rdv.profile.adapter.out.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prdv.rdv.profile.application.port.output.DataExportPort;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Export de portabilite au format JSON (RGPD art. 20) : structure
 * arborescente, lisible par machine, dates en ISO-8601.
 *
 * <p>Un adapteur CSV ou FHIR Bundle peut etre ajoute sur le meme port : le
 * cas d'usage ne connait que l'arbre de donnees neutre.
 */
@Component
public class JsonDataExportAdapter implements DataExportPort {

    private final ObjectMapper objectMapper;

    public JsonDataExportAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ExportedData export(String format, Map<String, Object> data) {
        String requested = format == null ? "json" : format.trim().toLowerCase(java.util.Locale.ROOT);
        if (!requested.equals("json")) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Format d'export non pris en charge : " + format + " (formats : " + supportedFormats() + ")");
        }
        try {
            String content = objectMapper.writeValueAsString(data);
            return new ExportedData("prdv-export-" + LocalDate.now() + ".json", "application/json", content);
        } catch (JsonProcessingException e) {
            throw ProfileException.of(ProfileErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "Generation de l'export impossible : " + e.getOriginalMessage());
        }
    }

    @Override
    public String supportedFormats() {
        return "json";
    }
}
