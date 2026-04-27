package com.contabilidade.pj.pendencia.service;

import com.contabilidade.pj.pendencia.entity.EntregaDocumento;
import java.util.List;

/** Resultado de envio em lote (melhor esforço: cada arquivo em transação própria). */
public record EntregaLoteResultado(List<EntregaDocumento> sucesso, List<EntregaLoteFalha> falhas) {}
