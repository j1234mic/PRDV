package com.prdv.adapters.out.persistence.json;

import com.prdv.schedule.domain.model.WeeklyAvailabilityRule;
import jakarta.persistence.Converter;

@Converter
public class WeeklyAvailabilityRuleListConverter extends JsonListAttributeConverter<WeeklyAvailabilityRule> {
    @Override
    protected Class<WeeklyAvailabilityRule> itemType() {
        return WeeklyAvailabilityRule.class;
    }
}
