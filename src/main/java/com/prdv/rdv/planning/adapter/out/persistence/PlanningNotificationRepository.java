package com.prdv.rdv.planning.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface PlanningNotificationRepository extends JpaRepository<PlanningNotificationEntity,UUID> {
    List<PlanningNotificationEntity> findByRecipientOrderByOccurredAtDescIdDesc(Long recipient,Pageable page);
    Optional<PlanningNotificationEntity> findByIdAndRecipient(UUID id,Long recipient);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from PlanningNotificationEntity n where n.deliveryState='PENDING' and n.nextAttemptAt<=:now order by n.nextAttemptAt,n.id")
    List<PlanningNotificationEntity> due(Instant now,Pageable page);
}
