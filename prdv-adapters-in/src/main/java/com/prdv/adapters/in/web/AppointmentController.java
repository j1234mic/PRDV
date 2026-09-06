package com.prdv.adapters.in.web;

import com.prdv.adapters.in.security.AuthenticatedUser;
import com.prdv.adapters.in.web.dto.ScheduleDtos.AppointmentResponse;
import com.prdv.adapters.in.web.dto.ScheduleDtos.BookingRequest;
import com.prdv.adapters.in.web.dto.ScheduleDtos.CancelRequest;
import com.prdv.adapters.in.web.dto.ScheduleDtos.RescheduleRequest;
import com.prdv.schedule.application.port.in.BookAppointmentUseCase;
import com.prdv.schedule.application.port.in.GetAppointmentsUseCase;
import com.prdv.schedule.application.port.in.ManageAppointmentUseCase;
import com.prdv.shared.exception.ForbiddenOperationException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Prise / gestion de rendez-vous (module 4.2).
 *
 * FRONTIERE TRANSACTIONNELLE : la classe est @Transactional -> le couple
 * (verrou pessimiste + verification de conflit + enregistrement + evenements
 * liste d'attente/notifications) est ATOMIQUE. C'est l'adaptateur entree qui ouvre
 * la transaction, le coeur reste propre (pas d'annotation dans prdv-core).
 */
@RestController
@RequestMapping("/api/appointments")
@Transactional(rollbackFor = Exception.class)
public class AppointmentController {

    private final BookAppointmentUseCase book;
    private final ManageAppointmentUseCase manage;
    private final GetAppointmentsUseCase queries;

    public AppointmentController(BookAppointmentUseCase book, ManageAppointmentUseCase manage,
                                 GetAppointmentsUseCase queries) {
        this.book = book;
        this.manage = manage;
        this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse book(@AuthenticationPrincipal AuthenticatedUser me,
                                    @Valid @RequestBody BookingRequest request) {
        requirePatient(me);
        return DtoMapper.toResponse(book.book(new BookAppointmentUseCase.Command(
                me.id(), request.doctorId(), request.start(), request.type(), request.reason())));
    }

    @GetMapping("/mine")
    public List<AppointmentResponse> mine(@AuthenticationPrincipal AuthenticatedUser me,
                                          @RequestParam(defaultValue = "30") int limit) {
        return queries.forPatient(me.id(), limit).stream().map(DtoMapper::toResponse).toList();
    }

    @GetMapping("/doctor")
    public List<AppointmentResponse> forDoctor(@AuthenticationPrincipal AuthenticatedUser me,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!me.isDoctor()) {
            throw new ForbiddenOperationException("Resume du jour reserve aux praticiens");
        }
        LocalDate day = date == null ? LocalDate.now() : date;
        return queries.forDoctorOn(me.id(), day).stream().map(DtoMapper::toResponse).toList();
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                      @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? null : request.reason();
        int feeCents = isPatientOf(me) ? manage.cancelByPatient(id, me.id(), reason) : 0;
        if (!isPatientOf(me)) {
            manage.cancelByDoctor(id, me.id(), reason);
        }
        return Map.of("cancellationFeeCents", feeCents);
    }

    @PostMapping("/{id}/reschedule")
    public AppointmentResponse reschedule(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                          @Valid @RequestBody RescheduleRequest request) {
        requirePatient(me);
        return DtoMapper.toResponse(manage.rescheduleByPatient(id, me.id(), request.newStart()));
    }

    @PostMapping("/{id}/confirm")
    public void confirm(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        manage.confirm(id, me.id());
    }

    @PostMapping("/{id}/no-show")
    public void noShow(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        manage.markNoShow(id, me.id());
    }

    // ----- helpers ------------------------------------------------------------

    private void requirePatient(AuthenticatedUser me) {
        if (!isPatientOf(me)) {
            throw new ForbiddenOperationException("Reservation reservee aux comptes patients");
        }
    }

    private boolean isPatientOf(AuthenticatedUser me) {
        return "PATIENT".equals(me.role());
    }
}
