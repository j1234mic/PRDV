package com.prdv.schedule.application.port.in;

import java.time.LocalDate;

/** S'inscrire / se retirer de la liste d'attente d'un praticien (module 4.3). */
public interface WaitingListUseCase {

    void join(Long patientUserId, Long doctorUserId, LocalDate earliest, LocalDate latest, boolean urgent);

    void leave(Long patientUserId, Long doctorUserId);
}
