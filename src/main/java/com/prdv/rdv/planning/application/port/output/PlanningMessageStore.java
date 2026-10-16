package com.prdv.rdv.planning.application.port.output;

import com.prdv.rdv.planning.application.port.input.PlanningInboxUseCase.*;
import java.time.Instant;
import java.util.*;

public interface PlanningMessageStore {
    List<Message> messages(Long user,int page,int size);
    void markRead(Long user,UUID id,Instant now);
    List<Audit> audit(Long practitioner,int page,int size);
}
