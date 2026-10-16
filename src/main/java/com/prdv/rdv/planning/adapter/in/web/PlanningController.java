package com.prdv.rdv.planning.adapter.in.web;

import com.prdv.rdv.planning.application.port.input.PlanningUseCase;
import com.prdv.rdv.planning.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/planning")
@PreAuthorize("isAuthenticated()")
public class PlanningController {
    private final PlanningUseCase planning;
    public PlanningController(PlanningUseCase planning) { this.planning=planning; }
    @PostMapping("/agenda")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public org.springframework.http.ResponseEntity<Void> open() {
        return planning.open()
                ? org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).build()
                : org.springframework.http.ResponseEntity.ok().build();
    }
    @GetMapping("/agenda/rules")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public Agenda.Rules rules() { return planning.rules(); }
    @GetMapping("/agenda/slots")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public List<Agenda.Slot> slots() { return planning.slots(); }
    @PutMapping("/agenda/rules")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public Agenda.Rules configure(@RequestBody Agenda.Rules rules) { return planning.configure(rules).rules(); }
    @PostMapping("/agenda/slots")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public List<Agenda.Slot> add(@RequestBody List<Agenda.Slot> slots) { return planning.add(slots).slots(); }
    @DeleteMapping("/agenda/slots/{id}")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public void removeSlot(@PathVariable UUID id) { planning.removeSlot(id); }
    @GetMapping("/{practitioner}/appointments/{id}")
    public PlanningUseCase.AppointmentDetails details(@PathVariable Long practitioner,@PathVariable UUID id) {
        return planning.details(practitioner,id);
    }
    @GetMapping(value="/{practitioner}/appointments/{id}/calendar",produces="text/calendar")
    public org.springframework.http.ResponseEntity<String> calendar(@PathVariable Long practitioner,@PathVariable UUID id) {
        var details=planning.details(practitioner,id);
        var format=java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC);
        String calendar="BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//PRDV//Planning//FR\r\nBEGIN:VEVENT\r\nUID:"
                + id + "@prdv\r\nDTSTAMP:" + format.format(details.appointment().bookedAt())
                + "\r\nDTSTART:" + format.format(details.slot().start()) + "\r\nDTEND:" + format.format(details.slot().end())
                + "\r\nSUMMARY:Rendez-vous medical\r\nSTATUS:"
                + (details.appointment().status()==Agenda.Status.CANCELLED ? "CANCELLED" :
                    details.appointment().status()==Agenda.Status.PENDING ? "TENTATIVE" : "CONFIRMED")
                + "\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        return org.springframework.http.ResponseEntity.ok().header("Content-Disposition","attachment; filename=rdv.ics")
                .header("Cache-Control","no-store").body(calendar);
    }
    @PostMapping("/agenda/patterns")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public List<Agenda.Slot> generate(@RequestBody SchedulePattern pattern) { return planning.generate(pattern).slots(); }
    @GetMapping("/{practitioner}/availability")
    public List<Agenda.Slot> available(@PathVariable Long practitioner, @RequestParam Instant from,
            @RequestParam Instant to, @RequestParam(required=false) String site,
            @RequestParam(defaultValue="false") boolean firstAvailable) {
        List<Agenda.Slot> slots=planning.available(practitioner,from,to,site);
        return firstAvailable ? slots.stream().limit(1).toList() : slots;
    }
    public record Booking(@NotNull UUID slot, @NotNull Agenda.Intake intake, boolean firstVisit) {}
    @PostMapping("/{practitioner}/appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public Agenda.Appointment book(@PathVariable Long practitioner,@Valid @RequestBody Booking request) {
        return planning.book(practitioner,request.slot(),request.intake(),request.firstVisit());
    }
    @GetMapping("/{practitioner}/appointments")
    public List<Agenda.Appointment> appointments(@PathVariable Long practitioner) { return planning.appointments(practitioner); }
    public record Cancellation(@NotBlank @Size(max=1000) String reason) {}
    @PostMapping("/{practitioner}/appointments/{id}/cancel")
    public Agenda.Appointment cancel(@PathVariable Long practitioner,@PathVariable UUID id,@Valid @RequestBody Cancellation request) {
        return planning.cancel(practitioner,id,request.reason());
    }
    public record Reschedule(@NotNull UUID slot, boolean firstVisit) {}
    @PostMapping("/{practitioner}/appointments/{id}/reschedule")
    public Agenda.Appointment reschedule(@PathVariable Long practitioner,@PathVariable UUID id,@Valid @RequestBody Reschedule request) {
        return planning.reschedule(practitioner,id,request.slot(),request.firstVisit());
    }
    public record Transition(@NotNull Agenda.Status status) {}
    @PostMapping("/{practitioner}/appointments/{id}/status")
    public Agenda.Appointment status(@PathVariable Long practitioner,@PathVariable UUID id,@Valid @RequestBody Transition request) {
        return planning.transition(practitioner,id,request.status());
    }
    public record WaitRequest(@NotNull Instant from,@NotNull Instant to,String site,@NotNull Agenda.Type type,boolean urgent,boolean automaticOffer,String communication) {}
    @PostMapping("/{practitioner}/waiting-list")
    @PreAuthorize("hasRole('PATIENT')")
    public Agenda.Waiting join(@PathVariable Long practitioner,@Valid @RequestBody WaitRequest request) {
        return planning.join(practitioner,request.from(),request.to(),request.site(),request.type(),request.urgent(),request.automaticOffer(),request.communication());
    }
    @GetMapping("/{practitioner}/waiting-list/offers")
    public List<WaitingOffer> offers(@PathVariable Long practitioner) { return planning.offers(practitioner); }
    public record OfferAcceptance(@NotNull Agenda.Intake intake) {}
    @PostMapping("/{practitioner}/waiting-list/offers/{offer}/accept")
    @PreAuthorize("hasRole('PATIENT')")
    public Agenda.Appointment accept(@PathVariable Long practitioner,@PathVariable UUID offer,@Valid @RequestBody OfferAcceptance request) {
        return planning.acceptOffer(practitioner,offer,request.intake());
    }
    @PostMapping("/{practitioner}/waiting-list/offers/{offer}/decline")
    @PreAuthorize("hasRole('PATIENT')")
    public void decline(@PathVariable Long practitioner,@PathVariable UUID offer) { planning.declineOffer(practitioner,offer); }
    @DeleteMapping("/{practitioner}/waiting-list/me")
    public void leave(@PathVariable Long practitioner) { planning.leave(practitioner); }
    @GetMapping("/agenda/slots/{slot}/candidates")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public List<Agenda.Waiting> candidates(@PathVariable UUID slot) { return planning.candidates(slot); }
    @PostMapping("/agenda/waiting-list/{waiting}/assign")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public Agenda.Appointment assign(@PathVariable UUID waiting,@Valid @RequestBody Booking request) {
        return planning.assign(waiting,request.slot(),request.intake(),request.firstVisit());
    }
    @GetMapping("/{practitioner}/queue")
    public List<PlanningUseCase.QueueEntry> queue(@PathVariable Long practitioner,@RequestParam(required=false) String site) {
        return planning.queue(practitioner,site);
    }
}
