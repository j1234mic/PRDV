package com.prdv.schedule.domain.policy;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Implementation canonique : annulation payante (et deplacement interdit) a moins de
 * <pre>lateWindow</pre> heures du RDV. FLEXIBLE = fenetre nulle, aucune penalite.
 */
public final class HourWindowCancellationPolicy implements CancellationPolicy {

    private final Duration lateWindow;
    private final int feeCents;

    public HourWindowCancellationPolicy(Duration lateWindow, int feeCents) {
        this.lateWindow = lateWindow;
        this.feeCents = Math.max(feeCents, 0);
    }

    @Override
    public boolean isLate(LocalDateTime slotStart, LocalDateTime now) {
        if (lateWindow.isZero()) {
            return false;
        }
        return !now.isBefore(slotStart.minus(lateWindow));
    }

    @Override
    public int cancellationFeeCents() {
        return feeCents;
    }
}
