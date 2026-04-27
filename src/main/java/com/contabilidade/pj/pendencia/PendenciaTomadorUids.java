package com.contabilidade.pj.pendencia;

/**
 * Identificador estável do tomador da pendência (empresa PJ ou cliente PF), usado na unicidade
 * (template + competência) sem ambiguidade entre CNPJ/CPF.
 */
public final class PendenciaTomadorUids {

    private PendenciaTomadorUids() {
    }

    public static String empresa(Long empresaId) {
        return "E:" + empresaId;
    }

    public static String clientePessoaFisica(Long clientePfId) {
        return "P:" + clientePfId;
    }
}
