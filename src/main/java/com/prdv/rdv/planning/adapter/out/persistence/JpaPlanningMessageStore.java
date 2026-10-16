package com.prdv.rdv.planning.adapter.out.persistence;

import com.prdv.rdv.planning.application.port.input.PlanningInboxUseCase.*;
import com.prdv.rdv.planning.application.port.output.PlanningMessageStore;
import com.prdv.rdv.planning.domain.PlanningException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Repository
public class JpaPlanningMessageStore implements PlanningMessageStore {
    private final PlanningNotificationRepository notifications;
    private final PlanningAuditRepository audits;
    public JpaPlanningMessageStore(PlanningNotificationRepository notifications,PlanningAuditRepository audits) {
        this.notifications=notifications; this.audits=audits;
    }
    @Transactional(readOnly=true)
    public List<Message> messages(Long user,int page,int size) {
        return notifications.findByRecipientOrderByOccurredAtDescIdDesc(user,PageRequest.of(page,size)).stream()
                .map(n -> new Message(n.id,n.type,n.practitioner,n.appointment,n.slot,n.occurredAt,n.readAt)).toList();
    }
    @Transactional
    public void markRead(Long user,UUID id,Instant now) {
        PlanningNotificationEntity n=notifications.findByIdAndRecipient(id,user)
                .orElseThrow(() -> new PlanningException(PlanningException.Code.NOT_FOUND,"Notification introuvable"));
        if(n.readAt==null) { n.readAt=now; notifications.save(n); }
    }
    @Transactional(readOnly=true)
    public List<Audit> audit(Long practitioner,int page,int size) {
        return audits.findByPractitionerOrderByOccurredAtDescIdDesc(practitioner,PageRequest.of(page,size)).stream()
                .map(a -> new Audit(a.id,a.actor,a.practitioner,a.action,a.occurredAt)).toList();
    }
}
