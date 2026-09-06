package com.prdv.adapters.out.persistence.schedule;

import com.prdv.schedule.application.port.out.DoctorScheduleRepository;
import com.prdv.schedule.domain.model.DoctorSchedule;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaDoctorScheduleRepository implements DoctorScheduleRepository {

    private final DoctorScheduleJpaRepository jpa;

    public JpaDoctorScheduleRepository(DoctorScheduleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DoctorSchedule save(DoctorSchedule schedule) {
        DoctorScheduleEntity e = new DoctorScheduleEntity();
        e.setId(schedule.id());
        e.setDoctorUserId(schedule.doctorUserId());
        e.setWeeklyRules(schedule.weeklyRules());
        e.setExceptions(schedule.exceptions());
        e.setMinNoticeHours(schedule.minNoticeHours());
        e.setMaxHorizonDays(schedule.maxHorizonDays());
        e.setCancellationMode(schedule.cancellationMode());
        e.setRequiresConfirmation(schedule.requiresManualConfirmation());
        DoctorScheduleEntity saved = jpa.save(e);
        if (schedule.id() == null) {
            schedule.assignId(saved.getId());
        }
        return schedule;
    }

    @Override
    public Optional<DoctorSchedule> findByDoctorUserId(Long doctorUserId) {
        return jpa.findByDoctorUserId(doctorUserId).map(JpaDoctorScheduleRepository::toDomain);
    }

    private static DoctorSchedule toDomain(DoctorScheduleEntity e) {
        return new DoctorSchedule(e.getId(), e.getDoctorUserId(), e.getWeeklyRules(), e.getExceptions(),
                e.getMinNoticeHours(), e.getMaxHorizonDays(), e.getCancellationMode(),
                e.isRequiresConfirmation());
    }
}
