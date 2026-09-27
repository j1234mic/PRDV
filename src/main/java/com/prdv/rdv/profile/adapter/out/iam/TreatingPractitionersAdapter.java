package com.prdv.rdv.profile.adapter.out.iam;

import com.prdv.rdv.profile.application.port.output.TreatingPractitionersPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Adapteur neutre : aucun praticien n'est encore considere comme « suivant »
 * le patient.
 *
 * <p>La regle de visibilite {@code MY_PRACTITIONERS} reste donc fermee par
 * defaut (principe du moindre privilege). Le module des rendez-vous
 * (module 3) fournira un adapteur reel alimente par les consultations a venir
 * et le medecin traitant declare, sans aucune modification des cas d'usage.
 */
@Component
public class TreatingPractitionersAdapter implements TreatingPractitionersPort {

    private static final Logger log = LoggerFactory.getLogger(TreatingPractitionersAdapter.class);

    @Override
    public Set<Long> practitionerUserIdsOf(Long patientUserId) {
        log.debug("Liste des praticiens suivis non encore alimentee pour le patient {}", patientUserId);
        return Set.of();
    }
}
