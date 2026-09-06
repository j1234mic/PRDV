package com.prdv.identity.application.port.out;

/** Port de sortie : algorithme de hash (implemente par un adaptateur BCrypt). */
public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hash);
}
