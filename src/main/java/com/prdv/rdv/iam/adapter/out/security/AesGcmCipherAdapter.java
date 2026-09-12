package com.prdv.rdv.iam.adapter.out.security;

import com.prdv.rdv.iam.adapter.out.persistence.security.EncryptionKeyHolder;
import com.prdv.rdv.iam.application.port.output.CipherPort;
import com.prdv.rdv.iam.config.IamProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Chiffrement symetrique authentifie AES-256-GCM.
 * Format : Base64( IV (12 octets) || ciphertext+tag ).
 * La cle (32 octets) est derivee du secret de configuration par PBKDF2.
 */
@Component
public class AesGcmCipherAdapter implements CipherPort {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 65_536;
    private static final byte[] SALT = "prdv-iam-aes-gcm".getBytes(StandardCharsets.UTF_8);

    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmCipherAdapter(IamProperties properties) {
        this.key = deriveKey(properties.getSecurity().getCipher().getSecret());
        EncryptionKeyHolder.initialize(this.key);
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        return encryptStatic(plainText, key);
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        return decryptStatic(cipherText, key);
    }

    public static String encryptStatic(String plainText, byte[] key) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv).put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Echec de chiffrement AES-GCM", e);
        }
    }

    public static String decryptStatic(String value, byte[] key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Echec de dechiffrement AES-GCM", e);
        }
    }

    private static byte[] deriveKey(String secret) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(secret.toCharArray(), SALT, PBKDF2_ITERATIONS, KEY_BITS);
            return factory.generateSecret(spec).getEncoded(); // 32 octets = AES-256
        } catch (Exception e) {
            throw new IllegalStateException("Derivation de cle impossible", e);
        }
    }
}
