package com.prdv.adapters.out.persistence.json;

import com.prdv.schedule.domain.model.ScheduleException;
import jakarta.persistence.Converter;

@Converter
public class ScheduleExceptionListConverter extends JsonListAttributeConverter<ScheduleException> {
    @Override
    protected Class<ScheduleException> itemType() {
        return ScheduleException.class;
    }
}
