package com.prdv.rdv.profile.adapter.out.storage;

import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Chiffrement au repos des fichiers medicaux (photos, pieces d'identite, scans
 * de carte Vitale, documents) : AES-256-GCM, IV aleatoire par fichier, contenu
 * authentifie (toute altération est detectee a la lecture).
 *
 * <p>Decorateur du port {@link MedicalFileStoragePort} : il enveloppe l'adapteur
 * de stockage (disque local en developpement, objet chiffre cote fournisseur
 * en production) sans que le domaine ni les cas d'usage ne le voient.
 *
 * <p>Format d'un fichier chiffre : {@code "PRDVENC1" | IV (12 octets) |
 * texte chiffre + tag GCM (16 octets)}.
 *
 * <p>Compatibilite : un fichier sans en-tete (depose avant l'activation du
 * chiffrement) est restitue tel quel. La cle est derivee par PBKDF2 de
 * {@code prdv.profile.storage.encryption-key} ; sa perte rend les fichiers
 * illisibles : elle doit etre conservee dans un coffre (variable
 * d'environnement en production).
 */
@Component
@Primary
public class EncryptedMedicalFileStorageAdapter implements MedicalFileStoragePort {

    static final byte[] MAGIC = "PRDVENC1".getBytes(StandardCharsets.US_ASCII);
    static final int IV_BYTES = 12;
    static final int TAG_BITS = 128;

    private static final int TAG_BYTES = TAG_BITS / 8;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KDF = "PBKDF2WithHmacSHA256";
    private static final int KDF_ITERATIONS = 65_536;
    private static final int KEY_BITS = 256;
    private static final byte[] KDF_SALT = "prdv-profile-storage-v1".getBytes(StandardCharsets.US_ASCII);

    private final MedicalFileStoragePort delegate;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptedMedicalFileStorageAdapter(
            @Qualifier("localMedicalFileStorageAdapter") MedicalFileStoragePort delegate,
            @Value("${prdv.profile.storage.encryption-key:}") String encryptionKey) {
        this.delegate = delegate;
        this.key = deriveKey(encryptionKey);
    }

    @Override
    public StoredFile store(String folder, String originalFilename, String contentType, byte[] content) {
        if (content == null || content.length == 0) {
            // Le delegue refuse le fichier vide avec le message metier habituel.
            return delegate.store(folder, originalFilename, contentType, content);
        }
        return delegate.store(folder, originalFilename, contentType, seal(content));
    }

    @Override
    public byte[] retrieve(String storageKey) {
        return open(delegate.retrieve(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        delegate.delete(storageKey);
    }

    /** Chiffre un contenu : en-tete, IV aleatoire, texte chiffre et tag. */
    byte[] seal(byte[] plain) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            byte[] sealed = cipher(Cipher.ENCRYPT_MODE, iv).doFinal(plain);
            return ByteBuffer.allocate(MAGIC.length + IV_BYTES + sealed.length)
                    .put(MAGIC)
                    .put(iv)
                    .put(sealed)
                    .array();
        } catch (GeneralSecurityException e) {
            throw ProfileException.of(ProfileErrorCode.STORAGE_FAILURE,
                    "Le chiffrement du fichier medical a echoue");
        }
    }

    /** Dechiffre un contenu ; sans en-tete, c'est un fichier anterieur restitue tel quel. */
    byte[] open(byte[] stored) {
        if (!hasHeader(stored)) {
            return stored;
        }
        if (stored.length < MAGIC.length + IV_BYTES + TAG_BYTES) {
            throw ProfileException.of(ProfileErrorCode.STORAGE_FAILURE, "Fichier medical chiffre tronque");
        }
        byte[] iv = Arrays.copyOfRange(stored, MAGIC.length, MAGIC.length + IV_BYTES);
        byte[] body = Arrays.copyOfRange(stored, MAGIC.length + IV_BYTES, stored.length);
        try {
            return cipher(Cipher.DECRYPT_MODE, iv).doFinal(body);
        } catch (GeneralSecurityException e) {
            throw ProfileException.of(ProfileErrorCode.STORAGE_FAILURE,
                    "Fichier medical illisible : contenu altere ou cle de chiffrement incorrecte");
        }
    }

    private static boolean hasHeader(byte[] stored) {
        return stored != null && stored.length >= MAGIC.length
                && Arrays.equals(Arrays.copyOf(stored, MAGIC.length), MAGIC);
    }

    private Cipher cipher(int mode, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        return cipher;
    }

    private static SecretKey deriveKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "prdv.profile.storage.encryption-key est obligatoire : les fichiers medicaux sont chiffres au repos");
        }
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF);
            byte[] raw = factory.generateSecret(
                    new PBEKeySpec(secret.toCharArray(), KDF_SALT, KDF_ITERATIONS, KEY_BITS)).getEncoded();
            return new SecretKeySpec(raw, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Derivation de la cle de chiffrement des fichiers impossible", e);
        }
    }
}
