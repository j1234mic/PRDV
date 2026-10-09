package com.prdv.rdv.profile.domain.model.identity;

import com.prdv.rdv.profile.domain.model.identity.PatientIdentity.Civility;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity.ContactPoint;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity.ContactType;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity.HealthInsurance;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity.InsuranceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Coordonnees verifiees et couvertures d'assurance du profil patient. */
class PatientIdentityContactsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("Le client ne peut pas declarer un contact comme verifie")
    void clientCannotDeclareAContactAsVerified() {
        PatientIdentity identity = identity();

        identity.replaceContacts(List.of(new ContactPoint(ContactType.MOBILE, "06 12 34 56 78", true, true)), CLOCK);

        List<ContactPoint> contacts = new ArrayList<>(identity.getContacts());
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).verified()).isFalse();
    }

    @Test
    @DisplayName("Un contact deja verifie reste verifie tant que sa valeur ne change pas")
    void verifiedContactStaysVerifiedOnlyWhileUnchanged() {
        PatientIdentity identity = identity();
        identity.setContacts(new LinkedHashSet<>(List.of(
                new ContactPoint(ContactType.MOBILE, "0612345678", true, true))));

        identity.replaceContacts(List.of(new ContactPoint(ContactType.MOBILE, "0612345678", false, true)), CLOCK);
        List<ContactPoint> unchanged = new ArrayList<>(identity.getContacts());
        assertThat(unchanged.get(0).verified()).isTrue();

        identity.replaceContacts(List.of(new ContactPoint(ContactType.MOBILE, "0699999999", false, true)), CLOCK);
        List<ContactPoint> changed = new ArrayList<>(identity.getContacts());
        assertThat(changed.get(0).verified()).isFalse();
    }

    @Test
    @DisplayName("Une couverture CMU ou AME ne remplace pas la complementaire ; une seule entree par type")
    void otherCoveragesDoNotOverwriteMainSlots() {
        PatientIdentity identity = identity();

        identity.updateInsurance(new HealthInsurance(InsuranceType.PRINCIPAL, "CPAM Paris", "1851275056123",
                null, null), CLOCK);
        identity.updateInsurance(new HealthInsurance(InsuranceType.COMPLEMENTARY, "Mutuelle Sante Plus",
                "M-123", "CT-9", null), CLOCK);
        identity.updateInsurance(new HealthInsurance(InsuranceType.UNIVERSAL_COVERAGE, "CPAM", "N-1",
                null, null), CLOCK);
        identity.updateInsurance(new HealthInsurance(InsuranceType.UNIVERSAL_COVERAGE, "CPAM", "N-2",
                null, null), CLOCK);

        assertThat(identity.getPrimaryInsurance().organization()).isEqualTo("CPAM Paris");
        assertThat(identity.getComplementaryInsurance().organization()).isEqualTo("Mutuelle Sante Plus");
        assertThat(identity.getOtherInsurances()).hasSize(1);
        assertThat(identity.getOtherInsurances().get(0).memberNumber()).isEqualTo("N-2");
    }

    @Test
    @DisplayName("L'effacement du profil supprime contacts, assurances complementaires et coordonnees")
    void erasureClearsContactsAndInsurances() {
        PatientIdentity identity = identity();
        identity.replaceContacts(List.of(new ContactPoint(ContactType.EMAIL, "claire@example.fr", false, true)),
                CLOCK);
        identity.updateInsurance(new HealthInsurance(InsuranceType.UNIVERSAL_COVERAGE, "CPAM", "N-1",
                null, null), CLOCK);

        identity.erase(CLOCK);

        assertThat(identity.getContacts()).isEmpty();
        assertThat(identity.getOtherInsurances()).isEmpty();
        assertThat(identity.getCivilStatus().lastName()).isEqualTo("ANONYMISE");
    }

    private static PatientIdentity identity() {
        return PatientIdentity.create(7L, new PatientIdentity.CivilStatus(Civility.MME, "Claire", null,
                "Durand", null, null, null, null, null, null, null), CLOCK);
    }
}
