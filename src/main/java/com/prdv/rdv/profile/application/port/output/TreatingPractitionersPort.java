package com.prdv.rdv.profile.application.port.output;

import java.util.Set;

/**
 * Port de sortie : praticiens qui suivent actuellement un patient
 * (medecin traitant declare + praticiens ayant un rendez-vous a venir).
 *
 * <p>Alimente la regle de visibilite {@code MY_PRACTITIONERS}. Le module des
 * rendez-vous (module 3) fournira l'adapteur reel ; l'adapteur actuel se
 * limite au medecin traitant declare dans l'identite du patient.
 */
public interface TreatingPractitionersPort {

    Set<Long> practitionerUserIdsOf(Long patientUserId);
}
