package com.prdv.identity.application.port.out;

import com.prdv.identity.domain.model.User;

import java.util.Optional;

/**
 * Port de sortie (DRIVEN PORT) - pattern REPOSITORY.
 * Le domaine exprime BESOIN de stocker des users ; l'adaptateur JPA/MySQL fournit.
 * Grace a l'inversion de dependance, prdv-core ne connait ni Spring Data ni Hibernate.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
