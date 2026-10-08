package com.prdv.rdv.profile.domain.model.identity;

import com.prdv.rdv.profile.domain.model.identity.PatientIdentity.EmergencyContact;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyContactTest {

    @Test
    void normalizesMissingRelationshipToNullAndTrimsProvidedValue() {
        var blankRelationship = new EmergencyContact(" Marie ", " Martin ", "   ", "+33612345678", null);
        var providedRelationship = new EmergencyContact("Marie", "Martin", " Conjoint ",
                "+33612345678", null);

        assertThat(blankRelationship.firstName()).isEqualTo("Marie");
        assertThat(blankRelationship.lastName()).isEqualTo("Martin");
        assertThat(blankRelationship.relationship()).isNull();
        assertThat(providedRelationship.relationship()).isEqualTo("Conjoint");
    }
}
