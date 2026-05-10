package com.exampl.util;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SecurityUtils {

    // RSA-OAEP — criptografia assimétrica
    private static final String TRANSFORMATION = "RSA/ECB/OAEPPadding";

    // RSA-2048 com OAEP+SHA-256 suporta até 190 bytes de dado por bloco
    private static final int CHUNK_SIZE = 190;

    // Chave pública do cliente atual (enviada via POST /handshake)
    private static volatile PublicKey clientPublicKey = null;

    /**
     * Armazena a chave pública RSA do cliente (recebida no handshake).
     * @param base64 Chave pública no formato SPKI codificada em Base64
     */
    public static void setClientPublicKey(String base64) throws Exception {
        clientPublicKey = decodePublicKey(base64);
        System.out.println("🔑 Chave pública RSA do cliente registrada com sucesso.");
    }

    /**
     * Retorna true se uma chave pública do cliente já foi recebida.
     */
    public static boolean hasClientPublicKey() {
        return clientPublicKey != null;
    }

    /**
     * Criptografa dados usando a chave pública RSA do cliente armazenada.
     * Lança IllegalStateException se nenhum handshake foi realizado.
     */
    public static String encryptForClient(String data) throws Exception {
        if (clientPublicKey == null) {
            throw new IllegalStateException("Nenhuma chave pública de cliente registrada. Faça o handshake primeiro.");
        }
        return encryptWithPublicKey(data, clientPublicKey);
    }

    /**
     * Criptografa uma String com a chave pública RSA fornecida.
     *
     * Como RSA-2048 suporta no máximo 190 bytes por operação, o dado é
     * dividido em blocos de 190 bytes. Cada bloco é criptografado
     * individualmente e o resultado é um JSON array de strings Base64.
     */
    public static String encryptWithPublicKey(String data, PublicKey publicKey) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // Web Crypto API (JS) com hash "SHA-256" usa SHA-256 tanto para o Hash quanto para o MGF1.
        // O Java por padrão usa SHA-1 no MGF1, então precisamos forçar SHA-256 aqui.
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            new MGF1ParameterSpec("SHA-256"),
            PSource.PSpecified.DEFAULT
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

        // Retorna um JSON array: ["chunk1Base64", "chunk2Base64", ...]
        return "[" + String.join(",", chunks) + "]";
    }

    /**
     * Importa uma chave pública RSA recebida do cliente.
     * O cliente envia a chave no formato SPKI codificado em Base64
     * (padrão exportado pela Web Crypto API do browser).
     */
    public static PublicKey decodePublicKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }
}