package com.prdv.adapters.in.web;

import com.prdv.adapters.in.security.AuthenticatedUser;
import com.prdv.adapters.in.web.dto.ScheduleDtos.BlockDayRequest;
import com.prdv.adapters.in.web.dto.ScheduleDtos.RuleRequest;
import com.prdv.adapters.in.web.dto.ScheduleDtos.ScheduleResponse;
import com.prdv.adapters.in.web.dto.ScheduleDtos.SettingsRequest;
import com.prdv.schedule.application.port.in.UpdateScheduleUseCase;
import com.prdv.schedule.domain.policy.CancellationMode;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Configuration agenda du praticien connecte (module 4.1). */
@RestController
@RequestMapping("/api/schedule/me")
public class ScheduleController {

    private final UpdateScheduleUseCase schedule;
    private final com.prdv.schedule.application.port.in.GetScheduleUseCase getSchedule;

    public ScheduleController(UpdateScheduleUseCase schedule,
                              com.prdv.schedule.application.port.in.GetScheduleUseCase getSchedule) {
        this.schedule = schedule;
        this.getSchedule = getSchedule;
    }

    @GetMapping
    public ScheduleResponse current(@AuthenticationPrincipal AuthenticatedUser me) {
        return DtoMapper.toResponse(getSchedule.forDoctor(me.id()));
    }

    @PostMapping("/rules")
    public ScheduleResponse addRule(@AuthenticationPrincipal AuthenticatedUser me,
                                    @Valid @RequestBody RuleRequest request) {
        return DtoMapper.toResponse(schedule.addRule(me.id(), new UpdateScheduleUseCase.RuleDto(
                request.day(), request.start(), request.end(), request.durationMinutes())));
    }

    @DeleteMapping("/rules")
    public ScheduleResponse removeRule(@AuthenticationPrincipal AuthenticatedUser me,
                                      @RequestParam DayOfWeek day, @RequestParam LocalTime start) {
        return DtoMapper.toResponse(schedule.removeRule(me.id(), day, start));
    }

    @PostMapping("/blocks")
    public ScheduleResponse block(@AuthenticationPrincipal AuthenticatedUser me,
                                  @Valid @RequestBody BlockDayRequest request) {
        return DtoMapper.toResponse(schedule.blockDay(me.id(), request.date(), request.reason()));
    }

    @DeleteMapping("/blocks")
    public ScheduleResponse unblock(@AuthenticationPrincipal AuthenticatedUser me, @RequestParam java.time.LocalDate date) {
        return DtoMapper.toResponse(schedule.unblockDay(me.id(), date));
    }

    @PutMapping("/settings")
    public ScheduleResponse settings(@AuthenticationPrincipal AuthenticatedUser me,
                                     @Valid @RequestBody SettingsRequest request) {
        return DtoMapper.toResponse(schedule.updateSettings(me.id(), request.minNoticeHours(),
                request.maxHorizonDays(), CancellationMode.valueOf(request.cancellationMode()),
                request.requiresManualConfirmation()));
    }
}
