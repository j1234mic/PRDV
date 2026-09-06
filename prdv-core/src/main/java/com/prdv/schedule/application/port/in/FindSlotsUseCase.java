package com.prdv.schedule.application.port.in;

import com.prdv.schedule.domain.model.Slot;

import java.time.LocalDate;
import java.util.List;

public interface FindSlotsUseCase {

    /** Vue semaine/mois du module 4.2 etape 3. */
    List<Slot> findFreeSlots(Long doctorUserId, LocalDate from, int days);

    /** Bouton "Premier creneau disponible". */
    Slot firstAvailable(Long doctorUserId, LocalDate from);
}
