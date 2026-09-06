package com.prdv.adapters.out.persistence.schedule;

import com.prdv.schedule.domain.model.AppointmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface AppointmentJpaRepository extends JpaRepository<AppointmentEntity, Long> {

    /** Chevauchement sur l'agenda d'un medecin, statuts qui "occupent" le creneau. */
    @Query("""
            select a from AppointmentEntity a
            where a.doctorUserId = :doctor
              and a.status in :activeStatuses
              and a.start < :end and a.end > :start
            """)
    List<AppointmentEntity> findActiveOverlapping(@Param("doctor") Long doctorUserId,
                                                  @Param("activeStatuses") List<AppointmentStatus> activeStatuses,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("""
            select a from AppointmentEntity a
            where a.doctorUserId = :doctor and a.start >= :from and a.start < :to
            order by a.start asc
            """)
    List<AppointmentEntity> findForDoctorBetween(@Param("doctor") Long doctorUserId,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);

    List<AppointmentEntity> findByPatientUserIdOrderByStartDesc(Long patientUserId, Pageable pageable);
}
