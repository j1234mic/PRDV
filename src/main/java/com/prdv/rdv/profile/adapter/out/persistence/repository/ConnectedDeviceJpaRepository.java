package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.ConnectedDeviceEntity;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConnectedDeviceJpaRepository extends JpaRepository<ConnectedDeviceEntity, String> {

    List<ConnectedDeviceEntity> findByUserIdOrderByConnectedAtDesc(Long userId);

    /** Appairages existants pour un identifiant externe (toutes plateformes confondues). */
    List<ConnectedDeviceEntity> findByUserIdAndExternalDeviceId(Long userId, String externalDeviceId);

    void deleteByUserId(Long userId);
}
