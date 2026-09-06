package com.prdv.adapters.in.web;

import com.prdv.adapters.in.security.AuthenticatedUser;
import com.prdv.adapters.in.web.dto.ScheduleDtos.WaitlistRequest;
import com.prdv.schedule.application.port.in.WaitingListUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Liste d'attente (module 4.3). L'attribution, elle, est pilotee par l'evenement d'annulation. */
@RestController
@RequestMapping("/api/waitlist")
public class WaitingListController {

    private final WaitingListUseCase waitingList;

    public WaitingListController(WaitingListUseCase waitingList) {
        this.waitingList = waitingList;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void join(@AuthenticationPrincipal AuthenticatedUser me, @Valid @RequestBody WaitlistRequest request) {
        waitingList.join(me.id(), request.doctorId(), request.earliest(), request.latest(), request.urgent());
    }

    @DeleteMapping("/doctors/{doctorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long doctorId) {
        waitingList.leave(me.id(), doctorId);
    }
}
