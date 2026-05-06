package com.exampl;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class SecurityUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding"; // GCM é mais seguro que CBC
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_SIZE = 12; // Recomendado para GCM

    // A chave deve ter 16, 24 ou 32 bytes (para AES-128, 192 ou 256)
    // Em um sistema real, isso ficaria em uma variável de ambiente
    private static final String CHAVE_MESTRA = "12345678901234567890123456789012";

    public static String encrypt(String strToEncrypt) throws Exception {
        // IV aleatório a cada chamada — obrigatório em AES-GCM para evitar quebra de segurança
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec secretKey = new SecretKeySpec(CHAVE_MESTRA.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));

        // Formato final: Base64( IV[12 bytes] ++ cipherText ++ authTag[16 bytes] )
        byte[] payload = new byte[IV_SIZE + cipherText.length];
        System.arraycopy(iv,         0, payload, 0,       IV_SIZE);
        System.arraycopy(cipherText, 0, payload, IV_SIZE, cipherText.length);

        return Base64.getEncoder().encodeToString(payload);
    }

    public static String decrypt(String strToDecrypt) throws Exception {
        byte[] payload = Base64.getDecoder().decode(strToDecrypt);

        // Extrai o IV dos primeiros 12 bytes e o ciphertext do restante
        byte[] iv         = Arrays.copyOfRange(payload, 0,       IV_SIZE);
        byte[] cipherText = Arrays.copyOfRange(payload, IV_SIZE, payload.length);

        SecretKeySpec secretKey = new SecretKeySpec(CHAVE_MESTRA.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText, StandardCharsets.UTF_8);
    }
}