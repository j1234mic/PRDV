package com.prdv.rdv.profile.adapter.in.web.dto;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyContactRequestValidationTest {

    @Test
    void relationshipMayBeOmittedOrBlank() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            var missingRelationship = new PatientIdentityDtos.EmergencyContactRequest(
                    "Marie", "Martin", null, "+33612345678", null);
            var blankRelationship = new PatientIdentityDtos.EmergencyContactRequest(
                    "Marie", "Martin", "   ", "+33612345678", null);

            assertThat(validator.validate(missingRelationship)).isEmpty();
            assertThat(validator.validate(blankRelationship)).isEmpty();
        }
    }

    @Test
    void relationshipCannotExceedPersistenceColumnLength() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var request = new PatientIdentityDtos.EmergencyContactRequest(
                    "Marie", "Martin", "x".repeat(61), "+33612345678", null);

            assertThat(factory.getValidator().validate(request))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .containsExactly("relationship");
        }
    }
}
