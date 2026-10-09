package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Suppression des fichiers devenus orphelins (photo ou scan remplace, photo de
 * cabinet retiree, lieu supprime, effacement de compte).
 *
 * <p>La suppression n'a lieu qu'une fois la transaction validee : si
 * l'enregistrement echoue, l'ancien fichier reste reference et intact. Au mieux :
 * un echec de suppression est journalise et ne bloque jamais la mise a jour du profil.
 */
@Component
public class ReplacedFileCleaner {

    private static final Logger log = LoggerFactory.getLogger(ReplacedFileCleaner.class);

    private final MedicalFileStoragePort fileStorage;

    public ReplacedFileCleaner(MedicalFileStoragePort fileStorage) {
        this.fileStorage = fileStorage;
    }

    /** Fichier remplace par {@code currentKey} : l'ancien est supprime s'il differe du nouveau. */
    public void discardReplaced(String previousKey, String currentKey) {
        if (previousKey == null || previousKey.isBlank() || previousKey.equals(currentKey)) {
            return;
        }
        discardAfterCommit(List.of(previousKey));
    }

    /** Fichiers orphelins a supprimer une fois la transaction courante validee. */
    public void discardAfterCommit(Collection<String> storageKeys) {
        List<String> keys = storageKeys.stream()
                .filter(Objects::nonNull)
                .filter(key -> !key.isBlank())
                .distinct()
                .toList();
        if (keys.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    keys.forEach(ReplacedFileCleaner.this::deleteQuietly);
                }
            });
        } else {
            keys.forEach(this::deleteQuietly);
        }
    }

    private void deleteQuietly(String storageKey) {
        try {
            fileStorage.delete(storageKey);
        } catch (RuntimeException e) {
            log.warn("Fichier {} non supprime (orphelin a traiter) : {}", storageKey, e.getMessage());
        }
    }
}
