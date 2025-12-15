package com.example.sas.common.security.service;

import com.example.sas.common.security.exception.EncryptionException;
import com.example.sas.common.security.dto.EncryptionResult;
import com.example.sas.common.security.abstractions.EncryptionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesGcmEncryptionService implements EncryptionService {

    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    @Value("${app.encryption.aes256.key:}")
    private String aesKeyBase64;

    @Value("${app.encryption.hmac.key:}")
    private String hmacKeyBase64;

    @Value("${app.encryption.key-id:default}")
    private String keyId;

    private SecretKey aesKey;
    private SecretKey hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        try {
            if (aesKeyBase64 == null || aesKeyBase64.isBlank()) {
                throw new EncryptionException("AES key (app.encryption.aes256.key) is not configured");
            }
            if (hmacKeyBase64 == null || hmacKeyBase64.isBlank()) {
                throw new EncryptionException("HMAC key (app.encryption.hmac.key) is not configured");
            }
            byte[] aesBytes = Base64.getDecoder().decode(aesKeyBase64);
            if (aesBytes.length != 32) {
                throw new EncryptionException("AES key must be 32 bytes (base64-encoded)");
            }
            aesKey = new SecretKeySpec(aesBytes, "AES");

            byte[] hmacBytes = Base64.getDecoder().decode(hmacKeyBase64);
            hmacKey = new SecretKeySpec(hmacBytes, HMAC_ALGO);
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("Failed to decode encryption keys from Base64", e);
        }
    }

    @Override
    public EncryptionResult encrypt(byte[] plaintext) throws EncryptionException {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            String ctB64 = Base64.getEncoder().encodeToString(ciphertext);
            String ivB64 = Base64.getEncoder().encodeToString(iv);
            return new EncryptionResult(ctB64, ivB64, keyId);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    @Override
    public byte[] decrypt(EncryptionResult result) throws EncryptionException {
        try {
            byte[] iv = Base64.getDecoder().decode(result.getIvBase64());
            byte[] ciphertext = Base64.getDecoder().decode(result.getCiphertextBase64());

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }

    @Override
    public String hmacSha256(String input) throws EncryptionException {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(hmacKey);
            byte[] result = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new EncryptionException("HMAC computation failed", e);
        }
    }
}
