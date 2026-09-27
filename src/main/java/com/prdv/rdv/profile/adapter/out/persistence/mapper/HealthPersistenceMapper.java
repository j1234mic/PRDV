package com.prdv.rdv.profile.adapter.out.persistence.mapper;

import com.prdv.rdv.profile.adapter.out.persistence.entity.ConnectedDeviceEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.HealthAlertEntity;
import com.prdv.rdv.profile.adapter.out.persistence.entity.HealthMetricEntity;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import org.springframework.stereotype.Component;

/** Mapper domaine &lt;-&gt; entites JPA pour la sante connectee. */
@Component
public class HealthPersistenceMapper {

    public ConnectedDeviceEntity toEntity(ConnectedDevice device) {
        ConnectedDeviceEntity entity = new ConnectedDeviceEntity();
        entity.setId(device.getId());
        entity.setUserId(device.getUserId());
        entity.setType(device.getType());
        entity.setProvider(device.getProvider());
        entity.setExternalDeviceId(device.getExternalDeviceId());
        entity.setLabel(device.getLabel());
        entity.setModel(device.getModel());
        entity.setFirmwareVersion(device.getFirmwareVersion());
        entity.setStatus(device.getStatus());
        entity.setLastSyncError(device.getLastSyncError());
        entity.setConnectedAt(device.getConnectedAt());
        entity.setLastSyncAt(device.getLastSyncAt());
        entity.setSyncedMetricsCount(device.getSyncedMetricsCount());
        return entity;
    }

    public ConnectedDevice toDomain(ConnectedDeviceEntity entity) {
        ConnectedDevice device = new ConnectedDevice();
        device.setId(entity.getId());
        device.setUserId(entity.getUserId());
        device.setType(entity.getType());
        device.setProvider(entity.getProvider());
        device.setExternalDeviceId(entity.getExternalDeviceId());
        device.setLabel(entity.getLabel());
        device.setModel(entity.getModel());
        device.setFirmwareVersion(entity.getFirmwareVersion());
        device.setStatus(entity.getStatus());
        device.setLastSyncError(entity.getLastSyncError());
        device.setConnectedAt(entity.getConnectedAt());
        device.setLastSyncAt(entity.getLastSyncAt());
        device.setSyncedMetricsCount(entity.getSyncedMetricsCount());
        return device;
    }

    public HealthMetricEntity toEntity(HealthMetric metric) {
        HealthMetricEntity entity = new HealthMetricEntity();
        entity.setId(metric.id());
        entity.setUserId(metric.userId());
        entity.setDeviceId(metric.deviceId());
        entity.setMetricType(metric.type());
        entity.setMetricValue(metric.value());
        entity.setContext(metric.context());
        entity.setRecordedAt(metric.recordedAt());
        entity.setImportedAt(metric.importedAt());
        entity.setSourceLabel(metric.sourceLabel());
        return entity;
    }

    public HealthMetric toDomain(HealthMetricEntity entity) {
        return new HealthMetric(entity.getId(), entity.getUserId(), entity.getDeviceId(),
                entity.getMetricType(), entity.getMetricValue(), entity.getContext(),
                entity.getRecordedAt(), entity.getImportedAt(), entity.getSourceLabel());
    }

    public HealthAlertEntity toEntity(HealthAlert alert) {
        HealthAlertEntity entity = new HealthAlertEntity();
        entity.setId(alert.getId());
        entity.setUserId(alert.getUserId());
        entity.setMetricType(alert.getMetricType());
        entity.setObservedValue(alert.getObservedValue());
        entity.setThreshold(alert.getThreshold());
        entity.setBoundary(alert.getBoundary());
        entity.setSeverity(alert.getSeverity());
        entity.setMessage(alert.getMessage());
        entity.setTriggeredAt(alert.getTriggeredAt());
        entity.setAcknowledged(alert.isAcknowledged());
        entity.setAcknowledgedAt(alert.getAcknowledgedAt());
        return entity;
    }

    public HealthAlert toDomain(HealthAlertEntity entity) {
        HealthAlert alert = new HealthAlert();
        alert.setId(entity.getId());
        alert.setUserId(entity.getUserId());
        alert.setMetricType(entity.getMetricType());
        alert.setObservedValue(entity.getObservedValue());
        alert.setThreshold(entity.getThreshold());
        alert.setBoundary(entity.getBoundary());
        alert.setSeverity(entity.getSeverity());
        alert.setMessage(entity.getMessage());
        alert.setTriggeredAt(entity.getTriggeredAt());
        alert.setAcknowledged(entity.isAcknowledged());
        alert.setAcknowledgedAt(entity.getAcknowledgedAt());
        return alert;
    }
}
