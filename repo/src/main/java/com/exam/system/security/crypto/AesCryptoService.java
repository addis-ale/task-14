package com.exam.system.security.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256 encryption service for sensitive fields at rest.
 */
@Component
public class AesCryptoService {

    private static final int IV_SIZE = 12;
    private static final int GCM_TAG_BITS = 128;

    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesCryptoService(@Value("${app.security.aes-base64-key}") String base64Key) {
        this.key = Base64.getDecoder().decode(base64Key);
        if (this.key.length != 32) {
            throw new IllegalArgumentException("AES key must be 256-bit (32 bytes)");
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt", ex);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(payload);

            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt", ex);
        }
    }
}
