package com.contabilidade.pj.pendencia;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class TemplateDocumentoSchemaConfig {

    private static final Logger log = LoggerFactory.getLogger(TemplateDocumentoSchemaConfig.class);

    @Bean
    CommandLineRunner ajustarSchemaTemplatesDocumentos(JdbcTemplate jdbcTemplate) {
        return args -> {
            tornarColunaNullableSeNecessario(jdbcTemplate, "empresa_id");
            tornarColunaNullableSeNecessario(jdbcTemplate, "cliente_pessoa_fisica_id");
        };
    }

    private void tornarColunaNullableSeNecessario(JdbcTemplate jdbcTemplate, String coluna) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT IS_NULLABLE, COLUMN_TYPE " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = 'templates_documentos' " +
                            "AND COLUMN_NAME = ?",
                    coluna
            );
            if (rows.isEmpty()) {
                return;
            }
            String nullable = String.valueOf(rows.get(0).get("IS_NULLABLE"));
            String columnType = String.valueOf(rows.get(0).get("COLUMN_TYPE"));
            if ("YES".equalsIgnoreCase(nullable)) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE templates_documentos MODIFY COLUMN " + coluna + " " + columnType + " NULL"
            );
            log.warn("Schema ajustado: templates_documentos.{} alterada para NULL.", coluna);
        } catch (Exception ex) {
            log.debug("Nao foi possivel ajustar nulidade de templates_documentos.{}: {}", coluna, ex.getMessage());
        }
    }
}
