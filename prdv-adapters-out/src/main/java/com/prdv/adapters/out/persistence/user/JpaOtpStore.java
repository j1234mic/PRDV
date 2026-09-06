package com.prdv.adapters.out.persistence.user;

import com.prdv.identity.application.port.out.OtpStore;
import com.prdv.identity.application.port.out.StoredOtp;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/** Adaptateur MySQL du stockage OTP (delai de validite gere a la lecture). */
@Component
public class JpaOtpStore implements OtpStore {

    private final OtpJpaRepository repo;

    public JpaOtpStore(OtpJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(String email, String codeHash, LocalDateTime expiresAt) {
        OtpEntity entity = new OtpEntity();
        entity.setEmail(email.toLowerCase());
        entity.setCodeHash(codeHash);
        entity.setExpiresAt(expiresAt);
        repo.save(entity);
    }

    @Override
    public Optional<StoredOtp> find(String email) {
        return repo.findById(email.toLowerCase())
                .map(e -> new StoredOtp(e.getCodeHash(), e.getExpiresAt()));
    }

    @Override
    public void delete(String email) {
        repo.deleteById(email.toLowerCase());
    }
}
