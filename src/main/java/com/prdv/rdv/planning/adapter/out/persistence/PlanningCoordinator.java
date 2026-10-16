package com.prdv.rdv.planning.adapter.out.persistence;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verrou d'ecriture global du planning.
 *
 * <p>Toute mutation d'agenda passe par {@link #lock()} : sans transaction active, ou si la
 * ligne de verrou est absente, l'operation echouait auparavant en {@code IllegalStateException}
 * silencieux, traduit cote client par un 500 {@code INTERNAL_ERROR} sans aucune trace dans les
 * logs. Les deux cas sont desormais journalises explicitement, et une ligne de verrou manquante
 * est recreee a la volee (auto-reparation) au lieu de bloquer toutes les ecritures.
 */
@Component
public class PlanningCoordinator {
    private static final Logger log = LoggerFactory.getLogger(PlanningCoordinator.class);
    private static final long MUTEX_ID = 1L;
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;
    public PlanningCoordinator(EntityManager entityManager,PlatformTransactionManager manager) {
        this.entityManager=entityManager; this.transaction=new TransactionTemplate(manager);
    }
    @PostConstruct
    public void initialize() {
        try {
            transaction.executeWithoutResult(status -> ensureMutexRow());
        } catch (org.springframework.dao.DataIntegrityViolationException | jakarta.persistence.PersistenceException ex) {
            // Another application instance may have inserted the singleton concurrently.
            Boolean exists=transaction.execute(status -> entityManager.find(PlanningMutexEntity.class,MUTEX_ID)!=null);
            if(!Boolean.TRUE.equals(exists)) throw ex;
        }
    }
    /**
     * Prend le verrou d'ecriture dans la transaction courante.
     *
     * @throws IllegalStateException si aucune transaction n'est active (appel hors
     *         {@code @Transactional}) : le verrou pessimiste est alors impossible a poser.
     */
    public void lock() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            log.error("Verrou planning demande hors transaction : la methode appelante doit etre "
                    + "@Transactional (sinon le verrou pessimiste est impossible a poser)");
            throw new IllegalStateException("Verrou planning demande hors d'une transaction active");
        }
        if(entityManager.find(PlanningMutexEntity.class,MUTEX_ID,LockModeType.PESSIMISTIC_WRITE)==null) {
            // Ligne absente (base restauree, table videe, migration partielle) : on la recree
            // plutot que de laisser toutes les ecritures du planning echouer definitivement.
            log.warn("Verrou planning absent (id={}) : recreation de la ligne de verrou", MUTEX_ID);
            ensureMutexRow();
            entityManager.flush();
            if(entityManager.find(PlanningMutexEntity.class,MUTEX_ID,LockModeType.PESSIMISTIC_WRITE)==null) {
                log.error("Verrou planning introuvable apres recreation : ecriture impossible");
                throw new IllegalStateException("Verrou planning absent");
            }
        }
    }
    private void ensureMutexRow() {
        if(entityManager.find(PlanningMutexEntity.class,MUTEX_ID)==null) {
            entityManager.persist(new PlanningMutexEntity(MUTEX_ID)); entityManager.flush();
        }
    }
}
