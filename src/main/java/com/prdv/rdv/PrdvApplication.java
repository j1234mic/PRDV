package com.prdv.rdv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entree de l'application PRDV.
 *
 * <p>Module 1 : Gestion des utilisateurs &amp; authentification.
 * L'architecture hexagonale (ports &amp; adapters) est organisee autour du package
 * {@code com.prdv.rdv.iam} :
 * <ul>
 *     <li>{@code domain} : modele metier pur, sans framework (entites, valeur, regles, evenements)</li>
 *     <li>{@code application} : cas d'usage (ports entrants) et ports sortants</li>
 *     <li>{@code adapter.in} : adapteurs acteurs (REST, securite web)</li>
 *     <li>{@code adapter.out} : adapteurs conduits (JPA/MySQL, JWT, OTP, API externes...)</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PrdvApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(PrdvApplication.class, args);
        } catch (Exception e) {
            // Le FailureAnalyzer dedie affichera deja un message clair pour les erreurs MySQL,
            // mais on ajoute un rappel minimal ici au cas ou l'analyseur ne serait pas charge.
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            String msg = root.getMessage() != null ? root.getMessage().toLowerCase() : "";
            if (msg.contains("communications link failure")
                    || msg.contains("connexion refusée")
                    || msg.contains("connection refused")
                    || msg.contains("jdbcconnectionexception")) {
                System.err.println("\n=================================================================");
                System.err.println(" ERREUR : Impossible de se connecter a MySQL (Connexion refusee)");
                System.err.println("=================================================================");
                System.err.println(" 1) Demarrez MySQL : docker compose up -d");
                System.err.println(" 2) Ou lancez sans MySQL avec H2 :");
                System.err.println("    mvn spring-boot:run -Dspring-boot.run.profiles=h2");
                System.err.println("    mvn spring-boot:run -Dspring-boot.run.profiles=dev");
                System.err.println("=================================================================\n");
            }
            throw e;
        }
    }
}
