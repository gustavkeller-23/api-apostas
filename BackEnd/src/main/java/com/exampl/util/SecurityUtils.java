package com.exampl.util;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SecurityUtils {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPPadding";
    private static final int CHUNK_SIZE = 190;

    // ─────────────────────────────────────────────────────────────
    // CHAVE DO SERVIDOR  (gerada uma vez ao iniciar o servidor)
    // O servidor usa sua chave PRIVADA para descriptografar o login
    // O browser usa a chave PÚBLICA do servidor para criptografar
    // ─────────────────────────────────────────────────────────────
    private static final KeyPair SERVER_KEY_PAIR = gerarParServidor();

    private static KeyPair gerarParServidor() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048, new SecureRandom());
            KeyPair kp = gen.generateKeyPair();
            System.out.println("🔐 Par de chaves RSA do SERVIDOR gerado com sucesso.");
            return kp;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar par de chaves do servidor", e);
        }
    }

    /**
     * Retorna a chave PÚBLICA do servidor em Base64 (formato SPKI).
     * Será enviada ao browser via GET /chave-publica.
     */
    public static String getServerPublicKeyBase64() {
        byte[] spki = SERVER_KEY_PAIR.getPublic().getEncoded();
        return Base64.getEncoder().encodeToString(spki);
    }

    /**
     * Descriptografa dados que vieram do browser cifrados com a chave pública do servidor.
     * Recebe um JSON array de chunks Base64: ["chunk1","chunk2",...]
     */
    public static String decryptWithServerPrivateKey(String chunksJson) throws Exception {
        // Remove colchetes e divide por vírgula
        String inner = chunksJson.trim().replaceAll("^\\[|\\]$", "");
        String[] parts = inner.split(",");

        PrivateKey privateKey = SERVER_KEY_PAIR.getPrivate();

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
            "SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);

        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String b64 = part.trim().replaceAll("\"", "");
            byte[] cipherBytes = Base64.getDecoder().decode(b64);
            byte[] plain = cipher.doFinal(cipherBytes);
            result.append(new String(plain, StandardCharsets.UTF_8));
        }
        return result.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // CHAVE DO CLIENTE  (enviada pelo browser via POST /handshake)
    // O servidor usa a chave PÚBLICA do cliente para criptografar
    // as respostas — só o browser consegue descriptografar
    // ─────────────────────────────────────────────────────────────
    private static volatile PublicKey clientPublicKey = null;

    public static void setClientPublicKey(String base64) throws Exception {
        clientPublicKey = decodePublicKey(base64);
        System.out.println("🔑 Chave pública RSA do cliente registrada com sucesso.");
    }

    public static boolean hasClientPublicKey() {
        return clientPublicKey != null;
    }

    public static String encryptForClient(String data) throws Exception {
        if (clientPublicKey == null) {
            throw new IllegalStateException("Nenhuma chave pública de cliente registrada. Faça o handshake primeiro.");
        }
        return encryptWithPublicKey(data, clientPublicKey);
    }

    /**
     * Criptografa dados com uma chave pública RSA.
     * Divide em blocos de 190 bytes (limite do RSA-2048 com OAEP-SHA256).
     * Retorna JSON array de chunks Base64: ["chunk1","chunk2",...]
     */
    public static String encryptWithPublicKey(String data, PublicKey publicKey) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
            "SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);

        List<String> chunks = new ArrayList<>();
        int offset = 0;
        while (offset < dataBytes.length) {
            int end = Math.min(offset + CHUNK_SIZE, dataBytes.length);
            byte[] chunk = new byte[end - offset];
            System.arraycopy(dataBytes, offset, chunk, 0, chunk.length);
            byte[] encrypted = cipher.doFinal(chunk);
            chunks.add("\"" + Base64.getEncoder().encodeToString(encrypted) + "\"");
            offset = end;
        }
        return "[" + String.join(",", chunks) + "]";
    }

    public static PublicKey decodePublicKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }
}