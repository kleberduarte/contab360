package com.contabilidade.pj.lgpd;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Criptografa CPF/CNPJ em repouso usando AES/CBC com IV determinístico
 * (derivado de SHA-256(chave || plaintext)), garantindo que o mesmo valor
 * sempre produza o mesmo ciphertext — o que preserva unicidade e buscas por igualdade.
 *
 * Compatível com dados legados (plaintext): valores que não são base64 válido
 * são retornados como estão e re-criptografados no próximo save.
 */
@Converter
public class CpfCnpjCryptoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] key = LgpdCryptoKeyHolder.getKey();
            byte[] iv = deriveIv(key, plaintext);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[16 + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, 16);
            System.arraycopy(encrypted, 0, combined, 16, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dado sensível LGPD", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            if (combined.length < 17) return dbData;
            byte[] iv = Arrays.copyOf(combined, 16);
            byte[] encrypted = Arrays.copyOfRange(combined, 16, combined.length);
            byte[] key = LgpdCryptoKeyHolder.getKey();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Dado não está criptografado (legado plaintext) — retorna como está.
            return dbData;
        }
    }

    /** IV = primeiros 16 bytes de SHA-256(key || plaintext) — determinístico. */
    private static byte[] deriveIv(byte[] key, String plaintext) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(key);
        sha.update(plaintext.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(sha.digest(), 16);
    }

    /** Utilitário para criptografar um valor de busca antes de passar ao repositório. */
    public static String encrypt(String plaintext) {
        return new CpfCnpjCryptoConverter().convertToDatabaseColumn(plaintext);
    }
}
