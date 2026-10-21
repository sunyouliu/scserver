package com.example.documentstore.security;

import com.example.documentstore.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SessionKeyCryptoService {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionKeyCryptoService(AppProperties properties) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(properties.security().sessionKeyEncryptionSecret().getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("无法初始化 session_key 加密服务", exception);
        }
    }

    public String encrypt(String plainText) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + cipherText.length)
                    .put(nonce)
                    .put(cipherText)
                    .array());
        } catch (Exception exception) {
            throw new IllegalStateException("session_key 加密失败", exception);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("session_key 解密失败", exception);
        }
    }
}
