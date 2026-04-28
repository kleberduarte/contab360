package com.contabilidade.pj.lgpd;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Centraliza a chave AES usada para criptografar CPF/CNPJ em repouso.
 * JPA AttributeConverters não recebem injeção do Spring diretamente,
 * por isso este holder estático é inicializado no startup.
 */
@Component
public class LgpdCryptoKeyHolder {

    private static byte[] KEY;

    @Value("${lgpd.crypto.key:}")
    private String keyBase64;

    @PostConstruct
    public void init() {
        if (keyBase64 == null || keyBase64.isBlank()) {
            // Chave fixa apenas para desenvolvimento local — NÃO usar em produção.
            KEY = "dev-lgpd-key-contab360-32-bytes!".getBytes(StandardCharsets.UTF_8);
        } else {
            KEY = Base64.getDecoder().decode(keyBase64);
            if (KEY.length != 16 && KEY.length != 24 && KEY.length != 32) {
                throw new IllegalStateException(
                        "lgpd.crypto.key deve ser uma chave AES válida (16, 24 ou 32 bytes em Base64).");
            }
        }
    }

    static byte[] getKey() {
        if (KEY == null) {
            throw new IllegalStateException("LgpdCryptoKeyHolder ainda não foi inicializado.");
        }
        return KEY;
    }
}
