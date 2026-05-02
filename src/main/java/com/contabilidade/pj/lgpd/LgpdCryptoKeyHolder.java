package com.contabilidade.pj.lgpd;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Centraliza a chave AES usada para criptografar CPF/CNPJ em repouso.
 * JPA AttributeConverters não recebem injeção do Spring diretamente,
 * por isso este holder estático é inicializado no startup.
 */
@Component
public class LgpdCryptoKeyHolder {

    private static byte[] KEY;

    private final Environment environment;
    private final String keyBase64;

    public LgpdCryptoKeyHolder(Environment environment, @Value("${lgpd.crypto.key:}") String keyBase64) {
        this.environment = environment;
        this.keyBase64 = keyBase64;
    }

    @PostConstruct
    public void init() {
        boolean perfilProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (keyBase64 == null || keyBase64.isBlank()) {
            if (perfilProd) {
                throw new IllegalStateException(
                        "LGPD_CRYPTO_KEY (lgpd.crypto.key) é obrigatório com o perfil 'prod' ativo.");
            }
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
