package com.prdv.adapters.out.persistence.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Convertisseur JPA generique : collections de value objects serialisees en JSON
 * dans une colonne TEXT. Choix assume : une plage d'agenda / une allergie ne s'indexe
 * pas seule, l'AGREGAT se recharge en entier (pattern AGREGAT > jointure SQL).
 */
public abstract class JsonListAttributeConverter<T> implements AttributeConverter<List<T>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Type des elements pour la deserialisation Jackson. */
    protected abstract Class<T> itemType();

    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Serialisation JSON impossible", e);
        }
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, itemType()));
        } catch (Exception e) {
            throw new IllegalStateException("Deserialisation JSON impossible", e);
        }
    }
}
