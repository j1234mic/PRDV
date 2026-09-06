package com.prdv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree unique. Le package racine com.prdv assure le component-scan
 * des adaptateurs (com.prdv.adapters.*) ; le COEUR (com.prdv.*) n'est pas annote :
 * il est cable a la main dans CoreBeansConfig -> zero dependance Spring dans prdv-core.
 */
@SpringBootApplication
public class PrdvApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrdvApplication.class, args);
    }
}
