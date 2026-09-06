package com.prdv.adapters.in.web;

import com.prdv.adapters.in.web.dto.AuthDtos.UserResponse;
import com.prdv.adapters.in.web.dto.ProfileDtos.DoctorProfileResponse;
import com.prdv.adapters.in.web.dto.ProfileDtos.PatientProfileResponse;
import com.prdv.adapters.in.web.dto.ProfileDtos.PracticeLocationDto;
import com.prdv.adapters.in.web.dto.ScheduleDtos.AppointmentResponse;
import com.prdv.adapters.in.web.dto.ScheduleDtos.ExceptionResponse;
import com.prdv.adapters.in.web.dto.ScheduleDtos.RuleResponse;
import com.prdv.adapters.in.web.dto.ScheduleDtos.ScheduleResponse;
import com.prdv.adapters.in.web.dto.ScheduleDtos.SlotResponse;
import com.prdv.identity.application.model.UserInfo;
import com.prdv.profile.domain.model.DoctorProfile;
import com.prdv.profile.domain.model.PatientProfile;
import com.prdv.schedule.domain.model.Appointment;
import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.Slot;

/** MAPPING explicite (anti-corruption layer) : le frontend ne voit jamais un objet JPA ni un agregat. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toResponse(UserInfo user) {
        return new UserResponse(user.id(), user.email(), user.role().name(), user.status().name());
    }

    public static PatientProfileResponse toResponse(PatientProfile p) {
        return new PatientProfileResponse(p.userId(), p.firstName(), p.lastName(), p.birthDate(), p.gender(),
                p.phone(), p.city(), maskSsn(p.socialSecurityNumber()), p.mutualInsurance(), p.allergies(),
                p.chronicConditions(), p.isMinor(), p.emergencyContactName());
    }

    public static DoctorProfileResponse toResponse(DoctorProfile d) {
        return new DoctorProfileResponse(d.userId(), d.fullName(), d.specialty(), d.subSpecialties(),
                d.description(), d.sector(), d.consultationFeeCents(), d.languages(),
                d.practiceLocations().stream()
                        .map(l -> new PracticeLocationDto(l.name(), l.address(), l.postalCode(), l.city(), l.phone()))
                        .toList(),
                d.verification().name(), d.rejectionReason());
    }

    public static ScheduleResponse toResponse(DoctorSchedule s) {
        return new ScheduleResponse(s.doctorUserId(),
                s.weeklyRules().stream()
                        .map(r -> new RuleResponse(r.day(), r.range().start(), r.range().end(), r.durationMinutes()))
                        .toList(),
                s.exceptions().stream()
                        .map(e -> new ExceptionResponse(e.date(), e.until(), e.reason()))
                        .toList(),
                s.minNoticeHours(), s.maxHorizonDays(), s.cancellationMode().name(), s.requiresManualConfirmation());
    }

    public static SlotResponse toResponse(Slot slot) {
        return new SlotResponse(slot.start(), slot.end(), slot.durationMinutes());
    }

    public static AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.id(), a.patientUserId(), a.doctorUserId(), a.slot().start(),
                a.slot().end(), a.slot().durationMinutes(), a.type().name(), a.status().name(), a.reason(),
                a.teleconsultationUrl(), a.cancellationFeeCents(), a.bookedBy());
    }

    /** 1 90 05 75 123 456 78 -> "19005******45678"-like, seuls 6 chiffres visibles. */
    static String maskSsn(String ssn) {
        if (ssn == null || ssn.length() < 8) {
            return null;
        }
        String digits = ssn.replaceAll("\\s", "");
        return digits.substring(0, 5) + "*".repeat(Math.max(0, digits.length() - 7))
                + digits.substring(digits.length() - 2);
    }
}
