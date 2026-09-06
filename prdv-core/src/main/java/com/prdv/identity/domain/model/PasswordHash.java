package com.prdv.identity.domain.model;

/**
 * Value Object : le hash du mot de passe (jamais le mot de passe en clair).
 * La strategie de hash (BCrypt aujourd'hui, Argon2 demain) est un port de sortie :
 * le domaine ne connait pas l'algorithme -> Open/Closed + Liskov (tout hacheur
 * implementant PasswordHasher est interchangeable).
 */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le hash de mot de passe est requis");
        }
    }
}
