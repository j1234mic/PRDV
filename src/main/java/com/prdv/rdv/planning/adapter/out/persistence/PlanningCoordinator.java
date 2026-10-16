package com.prdv.rdv.planning.adapter.out.persistence;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PlanningCoordinator {
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;
    public PlanningCoordinator(EntityManager entityManager,PlatformTransactionManager manager) {
        this.entityManager=entityManager; this.transaction=new TransactionTemplate(manager);
    }
    @PostConstruct
    public void initialize() {
        try {
            transaction.executeWithoutResult(status -> {
                if(entityManager.find(PlanningMutexEntity.class,1L)==null) {
                    entityManager.persist(new PlanningMutexEntity(1L)); entityManager.flush();
                }
            });
        } catch (org.springframework.dao.DataIntegrityViolationException | jakarta.persistence.PersistenceException ex) {
            // Another application instance may have inserted the singleton concurrently.
            Boolean exists=transaction.execute(status -> entityManager.find(PlanningMutexEntity.class,1L)!=null);
            if(!Boolean.TRUE.equals(exists)) throw ex;
        }
    }
    public void lock() {
        if(entityManager.find(PlanningMutexEntity.class,1L,LockModeType.PESSIMISTIC_WRITE)==null)
            throw new IllegalStateException("Verrou planning absent");
    }
}
