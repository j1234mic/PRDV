package com.prdv.adapters.out.persistence.schedule;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface DoctorScheduleJpaRepository extends JpaRepository<DoctorScheduleEntity, Long> {

    Optional<DoctorScheduleEntity> findByDoctorUserId(Long doctorUserId);

    /** SELECT ... FOR UPDATE : verrou applique sur l'agenda pendant la reservation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DoctorScheduleEntity s where s.doctorUserId = :doctorUserId")
    Optional<DoctorScheduleEntity> lockByDoctorUserId(@Param("doctorUserId") Long doctorUserId);
}
