package com.contabilidade.pj.fiscal;

public enum StatusCertificado {
    /** Pedido registrado; escritório ainda não avançou na AC. */
    EM_ANALISE,
    /** Aguardando biometria ou validação na Autoridade Certificadora. */
    AGUARDANDO_BIOMETRIA,
    /** Certificado emitido (arquivo/token disponível). */
    EMITIDO,
    /** Instalado no equipamento do cliente (quando aplicável). */
    INSTALADO,
    /** Aprovado internamente (legado; preferir EMITIDO). */
    APROVADO,
    /** Entregue / concluído para o cliente. */
    ENTREGUE,
    /** Pedido cancelado. */
    CANCELADO,
    /** Prazo próximo; renovação em aberto. */
    RENOVACAO_PENDENTE
}
