package com.prdv.schedule.application.port.out;

import com.prdv.schedule.domain.model.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port de sortie (REPOSITORY) enrichi de DEUX methodes de concurrence qui permettent
 * d'eviter le double-booking SANS que le domaine connaisse SQL :
 *  - lockDoctorSchedule : verrou optimiste -> le medecin comme "ressource sérialisée" pendant le booking
 *  - findBookedBetween : conflit de chevauchement.
 */
public interface AppointmentRepository {

    Appointment save(Appointment appointment);
    Optional<Appointment> findById(Long id);
    List<Appointment> findActiveOverlapping(Long doctorUserId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByPatient(Long patientUserId, int limit);
    List<Appointment> findByDoctorBetween(Long doctorUserId, LocalDateTime from, LocalDateTime to);

    /**
     * Verrouille la ligne "agenda" du medecin pour la transaction en cours (SELECT ... FOR UPDATE
     * dans l'adaptateur JPA). Deux reservations concurrentes du MEME creneau sont ainsi serialisees.
     */
    void lockDoctorSchedule(Long doctorUserId);
}
