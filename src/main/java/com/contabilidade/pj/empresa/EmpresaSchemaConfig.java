package com.contabilidade.pj.empresa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * CNPJ/CPF em {@code empresas} são criptografados (Base64) — colunas legadas VARCHAR(14)/VARCHAR(11) estouram no insert.
 */
@Configuration
public class EmpresaSchemaConfig {

    private static final Logger log = LoggerFactory.getLogger(EmpresaSchemaConfig.class);

    @Bean
    CommandLineRunner ajustarColunasCriptografiaEmpresa(JdbcTemplate jdbcTemplate) {
        return args -> garantirColunasCnpjCpf(jdbcTemplate);
    }

    private void garantirColunasCnpjCpf(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE empresas MODIFY COLUMN cnpj VARCHAR(255) NOT NULL"
            );
            log.info("Migration: coluna empresas.cnpj ajustada para VARCHAR(255).");
        } catch (Exception ex) {
            log.debug("empresas.cnpj já ajustada ou não foi possível alterar: {}", ex.getMessage());
        }
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE empresas MODIFY COLUMN cpf_responsavel VARCHAR(255) NULL"
            );
            log.info("Migration: coluna empresas.cpf_responsavel ajustada para VARCHAR(255).");
        } catch (Exception ex) {
            log.debug("empresas.cpf_responsavel já ajustada ou não foi possível alterar: {}", ex.getMessage());
        }
    }
}
