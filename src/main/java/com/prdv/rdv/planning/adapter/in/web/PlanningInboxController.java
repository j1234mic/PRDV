package com.prdv.rdv.planning.adapter.in.web;

import com.prdv.rdv.planning.application.port.input.PlanningInboxUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/planning")
@PreAuthorize("isAuthenticated()")
public class PlanningInboxController {
    private final PlanningInboxUseCase inbox;
    public PlanningInboxController(PlanningInboxUseCase inbox) { this.inbox=inbox; }
    @GetMapping("/notifications")
    public List<PlanningInboxUseCase.Message> messages(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return inbox.messages(page,size);
    }
    @PostMapping("/notifications/{id}/read")
    public void read(@PathVariable UUID id) { inbox.markRead(id); }
    @GetMapping("/agenda/audit")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public List<PlanningInboxUseCase.Audit> audit(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return inbox.audit(page,size);
    }
}
