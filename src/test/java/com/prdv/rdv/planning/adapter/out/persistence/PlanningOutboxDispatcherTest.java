package com.prdv.rdv.planning.adapter.out.persistence;

import com.prdv.rdv.planning.application.port.output.PlanningNotificationGateway;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PlanningOutboxDispatcherTest {
    final Instant now=Instant.parse("2026-10-16T08:00:00Z");
    @Test void failedTransportRetriesAndEventuallyDeadLettersWithoutPretendingSuccess() {
        var repository=mock(PlanningNotificationRepository.class);
        var gateway=mock(PlanningNotificationGateway.class);
        var message=new PlanningNotificationEntity(2L,1L,null,null,"SLOT_AVAILABLE",now);
        when(repository.due(any(),any())).thenReturn(List.of(message));
        doThrow(new IllegalStateException("SMTP absent")).when(gateway).send(any(),anyLong(),anyString());
        var dispatcher=new PlanningOutboxDispatcher(repository,gateway,Clock.fixed(now,ZoneOffset.UTC));
        assertThat(dispatcher.deliver()).isZero();
        assertThat(message.deliveryState).isEqualTo("PENDING");
        assertThat(message.nextAttemptAt).isAfter(now); assertThat(message.attempts).isEqualTo(1);
        message.attempts=7;
        assertThat(dispatcher.deliver()).isZero(); assertThat(message.deliveryState).isEqualTo("DEAD");
    }
    @Test void successfulTransportMarksMessageSent() {
        var repository=mock(PlanningNotificationRepository.class); var gateway=mock(PlanningNotificationGateway.class);
        var message=new PlanningNotificationEntity(2L,1L,null,null,"CALLED",now);
        when(repository.due(any(),any())).thenReturn(List.of(message));
        assertThat(new PlanningOutboxDispatcher(repository,gateway,Clock.fixed(now,ZoneOffset.UTC)).deliver()).isEqualTo(1);
        assertThat(message.deliveryState).isEqualTo("SENT");
        verify(gateway).send(message.id,2L,"CALLED");
    }
}
