package com.prdv.rdv.profile.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du module « profils &amp; gestion des donnees »
 * (prefixe {@code prdv.profile}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "prdv.profile")
public class ProfileProperties {

    private Documents documents = new Documents();
    private Media media = new Media();
    private Health health = new Health();
    private Consent consent = new Consent();
    private Dmp dmp = new Dmp();

    @Getter
    @Setter
    public static class Documents {
        /** Taille maximale d'un document medical (25 Mo par defaut). */
        private long maxBytes = 25L * 1024 * 1024;
        /** Repertoire de stockage des documents. */
        private String folder = "medical-documents";
        /** Fenetre d'anticipation des rappels de vaccination exposee par defaut. */
        private int reminderLookaheadDays = 60;
    }

    @Getter
    @Setter
    public static class Media {
        private long maxPhotoBytes = 5L * 1024 * 1024;
        private long maxVideoBytes = 100L * 1024 * 1024;
        private String patientFolder = "patient-media";
        private String practitionerFolder = "practitioner-media";
        private String locationFolder = "practice-locations";
    }

    @Getter
    @Setter
    public static class Health {
        /** Fenetre de synchronisation par defaut d'un objet connecte. */
        private int defaultSyncWindowDays = 30;
        /** Nombre maximal de mesures renvoyees par une requete de consultation. */
        private int maxMetricsPerQuery = 2_000;
        /** Taille maximale d'un import par lot. */
        private int maxMetricsPerBatch = 500;
    }

    @Getter
    @Setter
    public static class Consent {
        /** Version courante de la politique de confidentialite (preuve de consentement). */
        private String policyVersion = "2026-09";
    }

    @Getter
    @Setter
    public static class Dmp {
        /** Active la passerelle DMP (sinon les appels sont simules et journalises). */
        private boolean enabled = false;
    }
}
