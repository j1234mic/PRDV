package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.user.User;

import java.util.List;
import java.util.Optional;

/** Port sortant : persistence de l'agregat {@link User}. */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findAll(int page, int size);

    long count();
}
