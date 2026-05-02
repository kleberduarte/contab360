package com.contabilidade.pj.api;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacao(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> regraNegocio(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridade(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.error("DataIntegrityViolationException: {}", cause);
        String msg = "Não foi possível salvar os dados. Verifique os valores informados.";
        if (cause != null) {
            String c = cause.toLowerCase();
            if (c.contains("templates_documentos")
                    && (c.contains("duplicate entry") || c.contains("unique"))) {
                msg = "Ja existe um template com esse nome para este tomador (empresa ou pessoa fisica).";
            } else if (c.contains("duplicate entry") || c.contains("unique constraint")) {
                msg = "Registro duplicado: já existe um cadastro com esse valor único (por exemplo, CNPJ ou CPF já utilizado).";
            } else if (c.contains("foreign key constraint") || c.contains("cannot add or update")) {
                msg = "Não foi possível salvar: referência inválida ou registro relacionado inexistente.";
            } else if (c.contains("data too long") || c.contains("data truncation")) {
                msg = "Um ou mais campos excedem o tamanho permitido.";
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> erroGenerico(Exception ex) {
        log.error("Erro interno nao tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erro interno no servidor."));
    }
}
