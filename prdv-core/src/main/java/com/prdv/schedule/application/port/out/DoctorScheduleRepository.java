package com.prdv.schedule.application.port.out;

import com.prdv.schedule.domain.model.DoctorSchedule;

import java.util.Optional;

public interface DoctorScheduleRepository {
    DoctorSchedule save(DoctorSchedule schedule);
    Optional<DoctorSchedule> findByDoctorUserId(Long doctorUserId);
}
