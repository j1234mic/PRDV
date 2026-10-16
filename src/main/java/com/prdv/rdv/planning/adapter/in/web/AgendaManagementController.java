package com.prdv.rdv.planning.adapter.in.web;

import com.prdv.rdv.planning.application.port.input.PlanningUseCase;
import com.prdv.rdv.planning.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/planning")
@PreAuthorize("hasRole('PRACTITIONER')")
public class AgendaManagementController {
    private final PlanningUseCase planning;
    public AgendaManagementController(PlanningUseCase planning) { this.planning=planning; }
    @PostMapping(value="/agenda/import",consumes="text/calendar")
    public void importCalendar(@RequestParam String site,@RequestBody String content) { planning.importCalendar(content,site); }
    @GetMapping("/agenda/settings")
    public AgendaSettings settings() { return planning.settings(); }
    @PutMapping("/agenda/templates/{name}")
    public void saveTemplate(@PathVariable String name,@RequestBody SchedulePattern pattern) { planning.saveTemplate(name,pattern); }
    @DeleteMapping("/agenda/templates/{name}")
    public void deleteTemplate(@PathVariable String name) { planning.deleteTemplate(name); }
    public record Dates(@NotNull LocalDate from,@NotNull LocalDate until) {}
    @PostMapping("/agenda/templates/{name}/apply")
    public void apply(@PathVariable String name,@Valid @RequestBody Dates dates) { planning.applyTemplate(name,dates.from(),dates.until()); }
    @PutMapping("/agenda/reasons/{id}")
    public void saveReason(@PathVariable String id,@RequestBody ConsultationReason reason) { planning.saveReason(id,reason); }
    @DeleteMapping("/agenda/reasons/{id}")
    public void deleteReason(@PathVariable String id) { planning.deleteReason(id); }
    @GetMapping("/{practitioner}/reasons")
    @PreAuthorize("isAuthenticated()")
    public List<ConsultationReason> reasons(@PathVariable Long practitioner) { return planning.reasons(practitioner); }
    public record Duplication(@NotNull Instant from,@NotNull Instant to,@Min(1) @Max(730) int shiftDays) {}
    @PostMapping("/agenda/duplicate")
    public void duplicate(@Valid @RequestBody Duplication request) { planning.duplicate(request.from(),request.to(),request.shiftDays()); }
    public record Closure(@NotBlank String site,@NotNull Instant from,@NotNull Instant to,@NotNull Agenda.Type type) {}
    @PostMapping("/agenda/closures")
    public void close(@Valid @RequestBody Closure request) { planning.close(request.site(),request.from(),request.to(),request.type()); }
    public record Delay(@NotBlank String site,@Min(0) @Max(480) int minutes) {}
    @PutMapping("/agenda/delay")
    public void delay(@Valid @RequestBody Delay request) { planning.delay(request.site(),request.minutes()); }
}
