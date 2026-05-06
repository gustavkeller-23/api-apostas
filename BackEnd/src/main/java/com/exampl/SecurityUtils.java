package com.exampl;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
        byte[] iv = new byte[IV_SIZE]; // Em produção, use SecureRandom para gerar o IV
        //usando IV fixo
        SecretKeySpec secretKey = new SecretKeySpec(CHAVE_MESTRA.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decrypt(String strToDecrypt) throws Exception {
        byte[] iv = new byte[IV_SIZE];
        SecretKeySpec secretKey = new SecretKeySpec(CHAVE_MESTRA.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));

        return new String(plainText, StandardCharsets.UTF_8);
    }
}