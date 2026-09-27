package com.prdv.rdv.profile.application.port.output;

/**
 * Port de sortie : passerelle vers le Dossier Medical Partage national (DMP).
 *
 * <p>En production, l'adapteur appelle l'API « Mon espace sante » de la CNAM
 * (authentification CPS/patient). L'adapteur fourni simule les echanges et
 * journalise les appels.
 */
public interface DmpGatewayPort {

    /** Cree ou retrouve le DMP d'un patient et renvoie son identifiant. */
    String linkPatient(Long patientUserId, String socialSecurityNumberToken);

    /** Publie un document dans le DMP et renvoie la reference DMP. */
    String publishDocument(Long patientUserId, String dmpIdentifier, String documentTitle,
                           String contentType);

    /** Rapatrie les elements disponibles (nombre d'elements synchronises). */
    int pullUpdates(Long patientUserId, String dmpIdentifier);

    /** Retire le consentement de partage au niveau du DMP national. */
    void revokeSharing(Long patientUserId, String dmpIdentifier);
}
