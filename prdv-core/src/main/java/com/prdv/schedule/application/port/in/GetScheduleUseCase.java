package com.prdv.schedule.application.port.in;

import com.prdv.schedule.domain.model.DoctorSchedule;

public interface GetScheduleUseCase {
    DoctorSchedule forDoctor(Long doctorUserId);
}
