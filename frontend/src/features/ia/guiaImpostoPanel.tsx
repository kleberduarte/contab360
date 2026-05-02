import { useEffect } from "react";

type CampoDetalhado = { nome?: string; valor?: string; tipo?: string };
type SecaoDetalhada = { id?: string; titulo?: string; campos?: CampoDetalhado[] };

function toText(v: unknown): string {
  if (v == null) return "";
  return String(v);
}

function secaoCampos(secao: SecaoDetalhada): CampoDetalhado[] {
  if (!Array.isArray(secao.campos)) return [];
  return secao.campos;
}

function normalizarCampoNome(nome: string): string {
  return nome.trim().toLowerCase();
}

function normalizarBuscaCampo(nome: string): string {
  return nome
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function campoEhCompetencia(nomeCampo: string, tipoCampo?: string | null): boolean {
  const tipo = (tipoCampo || "").trim().toUpperCase();
  if (tipo === "COMPETENCIA") return true;
  const nome = normalizarBuscaCampo(nomeCampo);
  return nome.includes("competencia") || nome.includes("referencia");
}

function campoEhVencimento(nomeCampo: string, tipoCampo?: string | null): boolean {
  const nome = normalizarBuscaCampo(nomeCampo);
  const tipo = (tipoCampo || "").trim().toUpperCase();
  return nome.includes("vencimento") || nome.includes("data de vencimento") || (tipo === "DATA" && nome.includes("vcto"));
}

function campoEhDataDocumento(nomeCampo: string, tipoCampo?: string | null): boolean {
  const nome = normalizarBuscaCampo(nomeCampo);
  const tipo = (tipoCampo || "").trim().toUpperCase();
  if (campoEhVencimento(nomeCampo, tipoCampo) || campoEhCompetencia(nomeCampo, tipoCampo)) return false;
  return tipo === "DATA" || nome === "data" || nome.includes("data ");
}

function parseCompetencia(valor: string): { ano: number; mes: number } | null {
  const v = valor.trim();
  if (!v) return null;
  const yyyyMm = v.match(/^(\d{4})[-/](\d{1,2})$/);
  if (yyyyMm) {
    const ano = Number(yyyyMm[1]);
    const mes = Number(yyyyMm[2]);
    if (mes >= 1 && mes <= 12) return { ano, mes };
  }
  const mmYyyy = v.match(/^(\d{1,2})[-/](\d{4})$/);
  if (mmYyyy) {
    const mes = Number(mmYyyy[1]);
    const ano = Number(mmYyyy[2]);
    if (mes >= 1 && mes <= 12) return { ano, mes };
  }
  return null;
}

function vencimentoPorCompetencia(valorCompetencia: string): string {
  const comp = parseCompetencia(valorCompetencia);
  if (!comp) return "";
  const data = new Date(comp.ano, comp.mes, 10);
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
}

function secaoResponsavelInformacoes(titulo: string): boolean {
  const t = titulo.trim().toLowerCase();
  return t.includes("respons") && t.includes("informa");
}

function completarCamposSecao(secao: SecaoDetalhada, camposOriginais: CampoDetalhado[]): CampoDetalhado[] {
  if (!secaoResponsavelInformacoes(toText(secao.titulo))) return camposOriginais;
  const campos = [...camposOriginais];
  const existentes = new Set(campos.map((c) => normalizarCampoNome(toText(c.nome))));
  const esperados: CampoDetalhado[] = [
    { nome: "Nome", valor: "", tipo: "TEXTO" },
    { nome: "Data", valor: "", tipo: "DATA" },
    { nome: "Assinatura", valor: "", tipo: "TEXTO" }
  ];
  for (const esperado of esperados) {
    const chave = normalizarCampoNome(toText(esperado.nome));
    if (!existentes.has(chave)) {
      campos.push(esperado);
      existentes.add(chave);
    }
  }
  const ordem = new Map<string, number>([
    ["nome", 0],
    ["data", 1],
    ["assinatura", 2]
  ]);
  return [...campos].sort((a, b) => {
    const aNome = normalizarCampoNome(toText(a.nome));
    const bNome = normalizarCampoNome(toText(b.nome));
    const aOrd = ordem.get(aNome) ?? 99;
    const bOrd = ordem.get(bNome) ?? 99;
    if (aOrd !== bOrd) return aOrd - bOrd;
    return aNome.localeCompare(bNome);
  });
}

function classByTipo(tipo: string): string {
  const t = (tipo || "TEXTO").toUpperCase();
  if (t === "CNPJ" || t === "CPF") return "doc-tipo-chip-react doc-tipo-chip--id";
  if (t === "MOEDA") return "doc-tipo-chip-react doc-tipo-chip--moeda";
  if (t === "DATA") return "doc-tipo-chip-react doc-tipo-chip--data";
  return "doc-tipo-chip-react doc-tipo-chip--texto";
}

function labelByNome(nome: string): string {
  if (!nome) return "Campo";
  return nome
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/_/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

function formatarCpf(digits: string): string {
  const d = digits.replace(/\D/g, "").slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

function formatarCnpj(digits: string): string {
  const d = digits.replace(/\D/g, "").slice(0, 14);
  return d
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

function formatarMoeda(valor: string): string {
  const num = parseFloat(valor.replace(",", "."));
  if (isNaN(num)) return valor;
  return num.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function parseDataIso(valor: string): string | null {
  const v = (valor || "").trim();
  if (!v) return null;
  const iso = v.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (iso) return `${iso[1]}-${iso[2]}-${iso[3]}`;
  const br = v.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (br) return `${br[3]}-${br[2]}-${br[1]}`;
  return null;
}

function formatarDataBr(valor: string): string {
  const iso = parseDataIso(valor);
  if (!iso) return valor;
  const [ano, mes, dia] = iso.split("-");
  return `${dia}/${mes}/${ano}`;
}

function competenciaPorData(valorData: string): string {
  const iso = parseDataIso(valorData);
  if (!iso) return "";
  const [ano, mes] = iso.split("-");
  return `${ano}-${mes}`;
}

function formatByTipo(valor: string, tipo: string): string {
  const limpo = valor.trim();
  if (!limpo) return "Não identificado";
  const t = (tipo || "TEXTO").toUpperCase();
  if (t === "CPF") {
    const d = limpo.replace(/\D/g, "");
    return d.length === 11 ? formatarCpf(d) : limpo;
  }
  if (t === "CNPJ") {
    const d = limpo.replace(/\D/g, "");
    return d.length === 14 ? formatarCnpj(d) : limpo;
  }
  if (t === "MOEDA" || t === "VALOR") {
    return formatarMoeda(limpo);
  }
  if (t === "DATA") {
    return formatarDataBr(limpo);
  }
  return limpo;
}

export function GuiaImpostoPainelDetalhe({
  det,
  capturaPerfil,
  editavel,
  valores,
  tipoPorCampo,
  onValorChange
}: {
  det: Record<string, unknown> | null | undefined;
  capturaPerfil: string;
  editavel: boolean;
  valores: Record<string, string>;
  tipoPorCampo: Record<string, string | null>;
  onValorChange: (nome: string, valor: string) => void;
}) {
  if (!det || typeof det !== "object") return null;
  const secoes = Array.isArray(det.secoes) ? (det.secoes as SecaoDetalhada[]) : [];
  if (!secoes.length) return null;
  const camposEditaveis = secoes.flatMap((secao) =>
    completarCamposSecao(secao, secaoCampos(secao))
      .map((campo) => ({ nome: toText(campo.nome), tipo: toText(campo.tipo) || toText(tipoPorCampo[toText(campo.nome)] || "") }))
      .filter((campo) => Boolean(campo.nome))
  );

  useEffect(() => {
    if (!editavel) return;
    const competencias = camposEditaveis.filter((c) => campoEhCompetencia(c.nome, c.tipo));
    if (!competencias.length) return;
    const vencimentos = camposEditaveis.filter((c) => campoEhVencimento(c.nome, c.tipo));
    const competenciaExistente = competencias
      .map((c) => (valores[c.nome] || "").trim())
      .find((v) => Boolean(parseCompetencia(v)));
    const dataBase = camposEditaveis
      .filter((c) => campoEhDataDocumento(c.nome, c.tipo))
      .map((c) => (valores[c.nome] || "").trim())
      .find((v) => Boolean(parseDataIso(v)));
    const valorCompetencia = competenciaExistente || (dataBase ? competenciaPorData(dataBase) : "");
    if (!valorCompetencia || !parseCompetencia(valorCompetencia)) return;

    for (const comp of competencias) {
      if ((valores[comp.nome] || "").trim() !== valorCompetencia) {
        onValorChange(comp.nome, valorCompetencia);
      }
    }

    const vencimentoAuto = vencimentoPorCompetencia(valorCompetencia);
    if (!vencimentoAuto) return;
    for (const ven of vencimentos) {
      if ((valores[ven.nome] || "").trim() !== vencimentoAuto) {
        onValorChange(ven.nome, vencimentoAuto);
      }
    }
  }, [editavel, camposEditaveis, valores, onValorChange]);

  const placeholderByTipo = (tipo: string): string => {
    const base = (tipo || "TEXTO").trim().toUpperCase();
    if (base === "CNPJ") return "Ex.: 12.345.678/0001-95";
    if (base === "CPF") return "Ex.: 123.456.789-01";
    if (base === "MOEDA" || base === "VALOR") return "Ex.: 2500,00";
    if (base === "DATA") return "Ex.: 2026-04-29";
    return "Digite o valor";
  };

  return (
    <div className="holerite-painel-react guia-imposto-painel-react">
      {capturaPerfil === "GUIA_IMPOSTO_IRPF_COMPLETO" ? (
        <p className="holerite-badge-react guia-imposto-badge-react">IRPF · captura estruturada</p>
      ) : null}
      {secoes.map((secao, idx) => {
        const campos = completarCamposSecao(secao, secaoCampos(secao));
        return (
          <section key={secao.id || `secao-${idx}`} className="holerite-secao guia-imposto-secao-react">
            <h4 className="holerite-secao-titulo guia-imposto-secao-titulo-react">
              <span className="guia-imposto-secao-indice-react">{idx + 1}</span>
              <span>{toText(secao.titulo) || `Secao ${idx + 1}`}</span>
            </h4>
            {!campos.length ? (
              <p className="muted-react">Sem campos detectados nesta seção.</p>
            ) : (
              <div className="guia-imposto-campos-grid-react" role="list" aria-label={toText(secao.titulo) || `Secao ${idx + 1}`}>
                {campos.map((campo, i) => (
                  <article key={`${toText(campo.nome)}-${i}`} className="guia-imposto-campo-card-react" role="listitem">
                    <div className="guia-imposto-campo-top-react">
                      <label className="guia-imposto-campo-label-react" htmlFor={`guia-campo-${idx}-${i}`}>
                        {labelByNome(toText(campo.nome))}
                      </label>
                      <span className={classByTipo(toText(campo.tipo))}>{toText(campo.tipo) || "TEXTO"}</span>
                    </div>
                    {(() => {
                      const nomeCampo = toText(campo.nome);
                      const tipoCampo = toText(campo.tipo) || toText(tipoPorCampo[nomeCampo] || "");
                      const valorAtual = valores[nomeCampo] ?? toText(campo.valor);
                      if (editavel && nomeCampo) {
                        const vencimentoCampo = campoEhVencimento(nomeCampo, tipoCampo);
                        const valorInputDate = parseDataIso(valorAtual) || "";
                        return (
                          <input
                            id={`guia-campo-${idx}-${i}`}
                            type={vencimentoCampo ? "date" : "text"}
                            className="dados-campo-valor-doc-react guia-imposto-campo-input-react"
                            value={vencimentoCampo ? valorInputDate : valorAtual}
                            placeholder={placeholderByTipo(tipoCampo)}
                            onChange={(e) => {
                              const novoValor = e.target.value;
                              onValorChange(nomeCampo, novoValor);
                              if (!campoEhCompetencia(nomeCampo, tipoCampo)) return;

                              for (const outroCampo of camposEditaveis) {
                                if (outroCampo.nome !== nomeCampo && campoEhCompetencia(outroCampo.nome, outroCampo.tipo)) {
                                  onValorChange(outroCampo.nome, novoValor);
                                }
                              }

                              const vencimentoAuto = vencimentoPorCompetencia(novoValor);
                              if (!vencimentoAuto) return;
                              for (const outroCampo of camposEditaveis) {
                                if (campoEhVencimento(outroCampo.nome, outroCampo.tipo)) {
                                  onValorChange(outroCampo.nome, vencimentoAuto);
                                }
                              }
                            }}
                          />
                        );
                      }
                      return <p className="guia-imposto-campo-valor-react">{formatByTipo(valorAtual, tipoCampo)}</p>;
                    })()}
                  </article>
                ))}
              </div>
            )}
          </section>
        );
      })}
    </div>
  );
}
