package com.prdv.rdv.planning.adapter.out.persistence;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AgendaJpaRepository extends JpaRepository<AgendaEntity,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AgendaEntity a where a.practitionerId = :id")
    Optional<AgendaEntity> lockById(Long id);
}
