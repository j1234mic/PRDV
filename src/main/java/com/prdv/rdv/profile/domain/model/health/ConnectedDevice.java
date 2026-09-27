package com.prdv.rdv.profile.domain.model.health;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Objet connecte appaire au compte d'un patient (montre, tensiometre,
 * glucometre, balance, oxymetre, ECG portable...).
 *
 * <p>La synchronisation reelle est portee par un adapteur
 * ({@code ConnectedHealthProviderPort} : Apple Health, Google Fit, BLE...) ;
 * l'agregat ne connait que l'etat de la liaison et la date du dernier
 * rafraichissement.
 */
@Getter
@Setter
public class ConnectedDevice {

    public enum DeviceType {
        WEARABLE_WATCH,
        BLOOD_PRESSURE_MONITOR,
        GLUCOMETER,
        SMART_SCALE,
        PULSE_OXIMETER,
        PORTABLE_ECG,
        THERMOMETER,
        SLEEP_TRACKER
    }

    public enum Provider { APPLE_HEALTH, GOOGLE_FIT, BLUETOOTH_SIG, MANUAL, OTHER }

    public enum Status { CONNECTED, DISCONNECTED, SYNC_ERROR }

    private String id;
    private Long userId;
    private DeviceType type;
    private Provider provider;
    private String externalDeviceId;
    private String label;
    private String model;
    private String firmwareVersion;
    private Status status;
    private String lastSyncError;
    private Instant connectedAt;
    private Instant lastSyncAt;
    private long syncedMetricsCount;

    public static ConnectedDevice connect(Long userId, DeviceType type, Provider provider,
                                          String externalDeviceId, String label, String model,
                                          Clock clock) {
        if (userId == null || type == null || provider == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le type d'objet connecte et la plateforme source sont obligatoires");
        }
        ConnectedDevice device = new ConnectedDevice();
        device.id = java.util.UUID.randomUUID().toString();
        device.userId = userId;
        device.type = type;
        device.provider = provider;
        device.externalDeviceId = externalDeviceId;
        device.label = label == null || label.isBlank() ? type.name() : label.trim();
        device.model = model;
        device.status = Status.CONNECTED;
        device.connectedAt = clock.instant();
        return device;
    }

    public boolean isConnected() {
        return status == Status.CONNECTED;
    }

    /** Compteur de mesures importees lors de la derniere synchronisation. */
    public int markSynchronized(int importedMetrics, Clock clock) {
        if (importedMetrics < 0) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le nombre de mesures importees ne peut pas etre negatif");
        }
        if (!isConnected()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "L'objet connecte n'est plus appaire : synchronisation impossible");
        }
        this.syncedMetricsCount += importedMetrics;
        this.lastSyncAt = clock.instant();
        this.lastSyncError = null;
        return importedMetrics;
    }

    public void markSyncFailed(String reason, Clock clock) {
        this.status = Status.SYNC_ERROR;
        this.lastSyncError = reason;
        this.lastSyncAt = clock.instant();
    }

    public void disconnect(Clock clock) {
        this.status = Status.DISCONNECTED;
        this.lastSyncAt = clock.instant();
    }
}
