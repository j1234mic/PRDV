package com.prdv.schedule.application.service;

import com.prdv.schedule.application.port.in.WaitingListUseCase;
import com.prdv.schedule.application.port.out.WaitingListRepository;
import com.prdv.schedule.domain.model.WaitingListEntry;
import com.prdv.shared.exception.ConflictException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Inscription / retrait liste d'attente (module 4.3). */
public final class WaitingListHandler implements WaitingListUseCase {

    private final WaitingListRepository repository;
    private final Clock clock;

    public WaitingListHandler(WaitingListRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void join(Long patientUserId, Long doctorUserId, LocalDate earliest, LocalDate latest, boolean urgent) {
        boolean already = repository.findOpenByDoctor(doctorUserId).stream()
                .anyMatch(e -> e.patientUserId().equals(patientUserId));
        if (already) {
            throw new ConflictException("Vous etes deja sur la liste d'attente de ce praticien");
        }
        repository.save(WaitingListEntry.join(patientUserId, doctorUserId, earliest, latest, urgent,
                LocalDateTime.now(clock)));
    }

    @Override
    public void leave(Long patientUserId, Long doctorUserId) {
        repository.deleteByPatientAndDoctor(patientUserId, doctorUserId);
    }
}
