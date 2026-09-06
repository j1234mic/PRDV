package com.prdv.schedule;

import com.prdv.schedule.domain.model.Appointment;
import com.prdv.schedule.domain.model.AppointmentStatus;
import com.prdv.schedule.domain.model.AppointmentType;
import com.prdv.schedule.domain.model.Slot;
import com.prdv.schedule.domain.policy.CancellationMode;
import com.prdv.schedule.domain.policy.CancellationPolicy;
import com.prdv.schedule.domain.policy.CancellationPolicyFactory;
import com.prdv.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regles d'annulation/deplacement (module 4.2) testees sans framework, en 100% pur. */
class AppointmentCancellationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 7, 8, 0);

    private Appointment appointmentAt(LocalDateTime start) {
        return Appointment.request(1L, 2L, new Slot(start, 20), AppointmentType.FOLLOW_UP,
                "controle", true, "PATIENT", NOW);
    }

    @Test
    void free_cancellation_before_deadline() {
        CancellationPolicy policy = CancellationPolicyFactory.forMode(CancellationMode.STANDARD_24H);
        Appointment a = appointmentAt(NOW.plusHours(30)); // > 24h
        a.cancelByPatient(policy, "empechement", NOW);
        assertEquals(AppointmentStatus.CANCELLED_BY_PATIENT, a.status());
        assertEquals(0, a.cancellationFeeCents());
    }

    @Test
    void late_cancellation_incurs_fee() {
        CancellationPolicy policy = CancellationPolicyFactory.forMode(CancellationMode.STANDARD_24H);
        Appointment a = appointmentAt(NOW.plusHours(10)); // < 24h
        a.cancelByPatient(policy, "tardif", NOW);
        assertEquals(1000, a.cancellationFeeCents()); // 10 EUR
    }

    @Test
    void flexible_mode_never_charges() {
        CancellationPolicy policy = CancellationPolicyFactory.forMode(CancellationMode.FLEXIBLE);
        Appointment a = appointmentAt(NOW.plusMinutes(30));
        a.cancelByPatient(policy, "tardif", NOW);
        assertEquals(0, a.cancellationFeeCents());
    }

    @Test
    void doctor_cancellation_has_no_fee_and_is_always_allowed_before_start() {
        Appointment a = appointmentAt(NOW.plusMinutes(5));
        a.cancelByDoctor("imprevu cabinet", NOW);
        assertEquals(AppointmentStatus.CANCELLED_BY_DOCTOR, a.status());
        assertEquals(0, a.cancellationFeeCents());
    }

    @Test
    void cannot_cancel_in_the_past() {
        Appointment a = appointmentAt(NOW.minusHours(1));
        CancellationPolicy policy = CancellationPolicyFactory.forMode(CancellationMode.STANDARD_24H);
        assertThrows(ConflictException.class, () -> a.cancelByPatient(policy, "x", NOW));
    }

    @Test
    void reschedule_blocked_in_late_window() {
        CancellationPolicy policy = CancellationPolicyFactory.forMode(CancellationMode.STANDARD_24H);
        Appointment a = appointmentAt(NOW.plusHours(5)); // dans la fenetre de 24h
        Slot newSlot = new Slot(NOW.plusDays(3).withHour(10).withMinute(0), 20);
        assertThrows(ConflictException.class, () -> a.rescheduleTo(newSlot, policy, NOW));

        Appointment early = appointmentAt(NOW.plusHours(30));
        early.rescheduleTo(newSlot, policy, NOW);
        assertEquals(newSlot, early.slot());
        assertTrue(early.status().occupiesSlot());
    }
}
