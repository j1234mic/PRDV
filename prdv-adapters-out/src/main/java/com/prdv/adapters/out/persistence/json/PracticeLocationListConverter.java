package com.prdv.adapters.out.persistence.json;

import com.prdv.profile.domain.model.PracticeLocation;
import jakarta.persistence.Converter;

@Converter
public class PracticeLocationListConverter extends JsonListAttributeConverter<PracticeLocation> {
    @Override
    protected Class<PracticeLocation> itemType() {
        return PracticeLocation.class;
    }
}
