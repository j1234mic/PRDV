package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.entity.ConnectedDeviceEntity;
import com.prdv.rdv.profile.adapter.out.persistence.mapper.HealthPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.ConnectedDeviceJpaRepository;
import com.prdv.rdv.profile.application.port.output.ConnectedDeviceRepository;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adapteur JPA des objets connectes. */
@Repository
public class ConnectedDevicePersistenceAdapter implements ConnectedDeviceRepository {

    private final ConnectedDeviceJpaRepository jpa;
    private final HealthPersistenceMapper mapper;

    public ConnectedDevicePersistenceAdapter(ConnectedDeviceJpaRepository jpa,
                                             HealthPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public ConnectedDevice save(ConnectedDevice device) {
        return mapper.toDomain(jpa.save(mapper.toEntity(device)));
    }

    @Override
    public Optional<ConnectedDevice> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ConnectedDevice> findByUserId(Long userId) {
        return jpa.findByUserIdOrderByConnectedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ConnectedDevice> findByUserIdAndExternalId(Long userId, String externalDeviceId) {
        if (externalDeviceId == null || externalDeviceId.isBlank()) {
            return Optional.empty();
        }
        return jpa.findByUserIdAndExternalDeviceId(userId, externalDeviceId).stream()
                .map(mapper::toDomain)
                .findFirst();
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }
}
