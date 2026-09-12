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
        SpringApplication.run(PrdvApplication.class, args);
    }
}
