package com.prdv.adapters.in.web;

import com.prdv.adapters.in.web.dto.ScheduleDtos.SlotResponse;
import com.prdv.schedule.application.port.in.FindSlotsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Disponibilites publiques (module 4.2 etape 3 : "Calendrier Disponibilites"). */
@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final FindSlotsUseCase slots;

    public SlotController(FindSlotsUseCase slots) {
        this.slots = slots;
    }

    @GetMapping("/doctors/{doctorId}")
    public List<SlotResponse> availability(@PathVariable Long doctorId,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(defaultValue = "14") int days) {
        LocalDate start = from == null ? LocalDate.now() : from;
        return slots.findFreeSlots(doctorId, start, days).stream().map(DtoMapper::toResponse).toList();
    }

    @GetMapping("/doctors/{doctorId}/first")
    public SlotResponse firstAvailable(@PathVariable Long doctorId,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        return DtoMapper.toResponse(slots.firstAvailable(doctorId, from == null ? LocalDate.now() : from));
    }
}
