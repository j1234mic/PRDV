package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;

import java.util.List;
import java.util.Optional;

/** Persistance des objets connectes appaires. */
public interface ConnectedDeviceRepository {

    ConnectedDevice save(ConnectedDevice device);

    Optional<ConnectedDevice> findById(String id);

    List<ConnectedDevice> findByUserId(Long userId);

    /** Recherche d'un doublon d'appairage (meme plateforme, meme identifiant externe). */
    Optional<ConnectedDevice> findByUserIdAndExternalId(Long userId, String externalDeviceId);

    void deleteByUserId(Long userId);
}
