package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.HealthCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

import java.util.List;

/** Cas d'usage : objets connectes et import de mesures de sante. */
public interface ConnectedHealthUseCase {

    List<ProfileViews.ConnectedDeviceView> myDevices();

    ProfileViews.ConnectedDeviceView connectDevice(HealthCommands.ConnectDevice command);

    void disconnectDevice(String deviceId);

    /** Synchronisation avec la plateforme source (Apple Health / Google Fit / BLE). */
    ProfileViews.SyncReportView synchronize(HealthCommands.SynchronizeDevice command);

    /** Import manuel ou par lot (montre, tensiometre, glucometre, balance, oxymetre, ECG). */
    ProfileViews.SyncReportView ingest(HealthCommands.IngestMetrics command);
}
