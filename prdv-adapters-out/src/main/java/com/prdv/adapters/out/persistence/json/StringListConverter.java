package com.prdv.adapters.out.persistence.json;

import jakarta.persistence.Converter;

/** Allergies, sous-specialites, langues... */
@Converter
public class StringListConverter extends JsonListAttributeConverter<String> {
    @Override
    protected Class<String> itemType() {
        return String.class;
    }
}
