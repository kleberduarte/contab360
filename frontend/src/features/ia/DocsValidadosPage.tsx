import { FormEvent, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { DocTabIconCarousel } from "../../components/docCarouselIcons";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";
import { HoleritePainelDetalhe } from "./holeritePanel";

type CampoExtraido = { nome: string; valor: string | null; tipo: string | null };

type DocumentoValidadoItem = {
  pendenciaId: number;
  processamentoId: number;
  templatePendenciaNome: string;
  nomeArquivoOriginal: string;
  tipoDocumentoDetectado: string;
  atualizadoEm: string;
  confianca: number | null;
  campos: CampoExtraido[];
  detalhamentoDocumento: Record<string, unknown> | null;
  capturaPerfil: string | null;
};

type AbaDocumentos = {
  idAba: string;
  titulo: string;
  documentos: DocumentoValidadoItem[];
};

type PayloadValidados = {
  empresaId: number | null;
  cnpj: string | null;
  razaoSocial: string | null;
  clientePessoaFisicaId: number | null;
  cpfClientePf: string | null;
  nomeClientePf: string | null;
  abas: AbaDocumentos[];
};

type EmpresaOpt = { id: number; cnpj: string; razaoSocial: string };
type ClientePfOpt = { id: number; cpf: string; nomeCompleto: string };

function formatarCnpj(digits: string): string {
  const d = digits.replace(/\D/g, "").slice(0, 14);
  return d
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

function formatarCpf(digits: string): string {
  const d = digits.replace(/\D/g, "").slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

function formatarLabelCampo(chave: string): string {
  if (!chave) return "";
  return chave
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

function classeChipTipoCampo(tipo: string | null | undefined): string {
  const base = (tipo || "TEXTO").trim().toUpperCase();
  if (base === "CNPJ" || base === "CPF") return "doc-tipo-chip-react doc-tipo-chip--id";
  if (base === "MOEDA" || base === "VALOR") return "doc-tipo-chip-react doc-tipo-chip--moeda";
  if (base === "DATA" || base === "COMPETENCIA") return "doc-tipo-chip-react doc-tipo-chip--data";
  if (base === "NUMERO" || base === "NUMÉRICO" || base === "NUMERICO") return "doc-tipo-chip-react doc-tipo-chip--numero";
  return "doc-tipo-chip-react doc-tipo-chip--texto";
}

function placeholderCampo(tipo: string | null | undefined): string {
  const base = (tipo || "TEXTO").trim().toUpperCase();
  if (base === "CNPJ") return "Ex.: 12345678000195";
  if (base === "CPF") return "Ex.: 12345678901";
  if (base === "MOEDA" || base === "VALOR") return "Ex.: 2500.00";
  if (base === "DATA") return "Ex.: 2026-04-21";
  if (base === "COMPETENCIA") return "Ex.: 2026-04";
  return "Preencha o valor do campo";
}

function idAbaFromPayload(a: AbaDocumentos): string {
  if (a.idAba) return String(a.idAba);
  const t = (a.titulo || "").toLowerCase();
  if (t.includes("nota")) return "NOTA_FISCAL";
  if (t.includes("holerite") || t.includes("folha")) return "FOLHA_PAGAMENTO";
  if (t.includes("extrato")) return "EXTRATO_BANCARIO";
  if (t.includes("recibo")) return "RECIBO_DESPESA";
  if (t.includes("guia")) return "GUIA_IMPOSTO";
  if (t.includes("contrato")) return "CONTRATO_SOCIAL";
  if (t.includes("ata")) return "ATA_REUNIAO";
  if (t.includes("declara")) return "DECLARACAO_ACESSORIA";
  if (t.includes("empréstimo") || t.includes("financiamento")) return "EMPRESTIMO_FINANCIAMENTO";
  if (t.includes("fluxo")) return "FLUXO_CAIXA";
  return "OUTROS";
}

function DocValidadoKpis({ doc, camposVisiveis }: { doc: DocumentoValidadoItem; camposVisiveis: CampoExtraido[] }) {
  const temDetalhe = doc.detalhamentoDocumento && typeof doc.detalhamentoDocumento === "object";
  const map: Record<string, string> = {};
  for (const c of camposVisiveis) {
    if (c.nome) map[c.nome] = c.valor ?? "";
  }
  let valorTitulo = "Valor";
  let valorVal = "—";
  if (temDetalhe) {
    const t = doc.detalhamentoDocumento as Record<string, unknown>;
    const tot = t.totais as Record<string, unknown> | undefined;
    if (tot && typeof tot === "object") {
      if (tot.valorLiquidoBr) {
        valorTitulo = "Valor líquido";
        valorVal = String(tot.valorLiquidoBr);
      } else if (tot.valorLiquidoNumerico != null) {
        valorTitulo = "Valor líquido";
        valorVal = String(tot.valorLiquidoNumerico);
      }
    }
  } else {
    if (map.valorLiquidoBr) {
      valorTitulo = "Valor líquido";
      valorVal = map.valorLiquidoBr;
    } else if (map.valorLiquidoNumerico) {
      valorTitulo = "Valor líquido";
      valorVal = map.valorLiquidoNumerico;
    }
  }
  const cnpjDig = map.cnpj || "";
  const cnpjFmt =
    cnpjDig && String(cnpjDig).replace(/\D/g, "").length === 14
      ? formatarCnpj(String(cnpjDig).replace(/\D/g, ""))
      : cnpjDig || "—";
  const comp = map.competencia || "—";
  const ven = map.vencimento || "—";

  return (
    <div className="doc-validado-kpis-react" role="group" aria-label="Resumo do documento">
      <div className="doc-validado-kpi-react">
        <span className="doc-validado-kpi__label">CNPJ</span>
        <span className="doc-validado-kpi__val">{cnpjFmt}</span>
      </div>
      <div className="doc-validado-kpi-react">
        <span className="doc-validado-kpi__label">{valorTitulo}</span>
        <span className="doc-validado-kpi__val doc-validado-kpi__val--destaque">{valorVal}</span>
      </div>
      <div className="doc-validado-kpi-react">
        <span className="doc-validado-kpi__label">Competência</span>
        <span className="doc-validado-kpi__val">{comp}</span>
      </div>
      <div className="doc-validado-kpi-react">
        <span className="doc-validado-kpi__label">Vencimento</span>
        <span className="doc-validado-kpi__val">{ven}</span>
      </div>
    </div>
  );
}

function DocValidadoCard({
  doc,
  editavel,
  sessao,
  onSalvo
}: {
  doc: DocumentoValidadoItem;
  editavel: boolean;
  sessao: Sessao;
  onSalvo: () => void;
}) {
  const holeritePrefix = "holerite.";
  const temDetalhe = doc.detalhamentoDocumento && typeof doc.detalhamentoDocumento === "object";
  const campos = doc.campos || [];
  const camposTabela = useMemo(
    () => (temDetalhe ? campos.filter((c) => !String(c.nome || "").startsWith(holeritePrefix)) : campos),
    [temDetalhe, campos]
  );
  const camposOcultos = useMemo(
    () => (temDetalhe ? campos.filter((c) => String(c.nome || "").startsWith(holeritePrefix)) : []),
    [temDetalhe, campos]
  );
  const camposEdicao = useMemo(() => [...camposTabela, ...camposOcultos], [camposTabela, camposOcultos]);

  const [valores, setValores] = useState<Record<string, string>>(() => {
    const o: Record<string, string> = {};
    for (const c of camposEdicao) {
      o[c.nome] = c.valor ?? "";
    }
    return o;
  });

  useEffect(() => {
    const o: Record<string, string> = {};
    for (const c of camposEdicao) {
      o[c.nome] = c.valor ?? "";
    }
    setValores(o);
  }, [doc.processamentoId, camposEdicao]);

  const conf = doc.confianca != null ? Number(doc.confianca).toFixed(2) : "—";
  const confPct = doc.confianca != null ? Math.round(Number(doc.confianca) * 100) : null;
  const captura = doc.capturaPerfil || "";
  const inicial = (doc.nomeArquivoOriginal || "Arquivo").slice(0, 1).toUpperCase();

  const [salvando, setSalvando] = useState(false);
  const [msg, setMsg] = useState<{ tipo: "ok" | "err"; text: string } | null>(null);

  async function salvar(ev: FormEvent) {
    ev.preventDefault();
    setMsg(null);
    setSalvando(true);
    const camposPayload = Object.entries(valores).map(([nome, valor]) => ({ nome, valor }));
    try {
      await apiFetchJson(`/api/inteligencia/documentos/${doc.processamentoId}/campos`, {
        method: "PATCH",
        body: JSON.stringify({ campos: camposPayload }),
        sessao
      });
      setMsg({ tipo: "ok", text: "Alterações salvas." });
      onSalvo();
    } catch (e) {
      setMsg({ tipo: "err", text: e instanceof Error ? e.message : "Falha ao salvar." });
    } finally {
      setSalvando(false);
    }
  }

  const tituloSec =
    temDetalhe && camposTabela.length
      ? "Campos para conferência e edição"
      : camposTabela.length
        ? "Campos extraídos"
        : "Conferência";

  return (
    <article className="doc-validado-card-react">
      <div className="doc-validado-card__top-react">
        <div className="doc-validado-card__identity-react">
          <div className="doc-validado-avatar-react" aria-hidden="true">
            {inicial}
          </div>
          <div className="doc-validado-card__titles-react">
            <h3 className="doc-validado-card__filename-react">{doc.nomeArquivoOriginal || "Arquivo"}</h3>
            <p className="doc-validado-card__pendencia-react muted-react">
              Pendência #{doc.pendenciaId} — {doc.templatePendenciaNome || ""}
            </p>
          </div>
        </div>
        <div className="doc-validado-card__meta-row-react">
          <div className="doc-validado-card__chips-react">
            <span className="doc-chip-react doc-chip--tipo" title="Tipo detectado pela IA">
              {doc.tipoDocumentoDetectado || "—"}
            </span>
            {captura === "HOLERITE_ESCRITORIO_COMPLETO" ? (
              <span className="doc-chip-react doc-chip--accent">Captura completa</span>
            ) : null}
            <span className="doc-chip-react doc-chip--conf" title="Confiança da leitura">
              {confPct != null ? `${confPct}%` : conf} confiança
            </span>
          </div>
          <p className="doc-validado-card__updated-react muted-react">Atualizado em {doc.atualizadoEm || "—"}</p>
        </div>
      </div>

      <DocValidadoKpis doc={doc} camposVisiveis={campos} />

      {temDetalhe && doc.detalhamentoDocumento ? (
        <div className="doc-validado-holerite-react">
          <HoleritePainelDetalhe det={doc.detalhamentoDocumento} capturaPerfil={captura} />
        </div>
      ) : null}

      <div className="doc-validado-card__body-react">
        {camposTabela.length > 0 || camposOcultos.length > 0 ? (
          <>
            <div className="doc-validado-secao-head-react">
              <h4 className="doc-validado-secao-titulo-react">{tituloSec}</h4>
              <span className="doc-validado-secao-count-react">{camposTabela.length} campo(s)</span>
            </div>
            <div className="doc-validado-table-wrap-react">
              {camposTabela.length > 0 ? (
                <div className="doc-campos-lista-react" role="table" aria-label="Campos extraídos">
                  <div className="doc-campos-lista-head-react" role="row">
                    <span role="columnheader">Campo</span>
                    <span role="columnheader">Valor</span>
                    <span role="columnheader">Tipo</span>
                  </div>
                  {camposTabela.map((c) => (
                    <div key={c.nome} className="doc-campo-item-react" role="row">
                      <div className="doc-campo-item-react__campo" role="cell">
                        <span className="doc-campo-item-react__label">Campo</span>
                        <strong>{formatarLabelCampo(c.nome)}</strong>
                      </div>
                      <div className="doc-campo-item-react__valor" role="cell">
                        <span className="doc-campo-item-react__label">Valor</span>
                        {editavel ? (
                          <input
                            type="text"
                            className="dados-campo-valor-doc-react"
                            value={valores[c.nome] ?? ""}
                            placeholder={placeholderCampo(c.tipo)}
                            onChange={(e) => setValores((prev) => ({ ...prev, [c.nome]: e.target.value }))}
                          />
                        ) : (
                          <span className="doc-campo-item-react__valor-texto">{c.valor ?? ""}</span>
                        )}
                      </div>
                      <div className="doc-campo-item-react__tipo" role="cell">
                        <span className="doc-campo-item-react__label">Tipo</span>
                        <span className={classeChipTipoCampo(c.tipo)}>
                          <span>{c.tipo || "TEXTO"}</span>
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : camposOcultos.length > 0 ? (
                <p className="muted-react doc-validado-inline-hint-react">
                  O detalhamento está no painel acima. Campos técnicos do holerite permanecem ao salvar.
                </p>
              ) : null}
            </div>
          </>
        ) : !temDetalhe ? (
          <p className="muted-react doc-validado-sem-campos-react">Nenhum campo extraído armazenado para este documento.</p>
        ) : null}
      </div>

      {editavel && (camposTabela.length > 0 || camposOcultos.length > 0) ? (
        <form className="doc-validado-card__actions-react" onSubmit={salvar}>
          <p className="doc-validado-card__actions-tip-react muted-react">
            Revise os campos e salve para atualizar a análise deste documento.
          </p>
          <button type="submit" className="ghost small-btn" disabled={salvando}>
            {salvando ? "Salvando..." : "Salvar alterações"}
          </button>
          {msg ? <span className={msg.tipo === "ok" ? "ok" : "erro"}>{msg.text}</span> : null}
        </form>
      ) : null}
    </article>
  );
}

export function DocsValidadosPage({ sessao }: { sessao: Sessao }) {
  const perfil = sessao.perfil;
  const [empresas, setEmpresas] = useState<EmpresaOpt[]>([]);
  const [clientesPf, setClientesPf] = useState<ClientePfOpt[]>([]);
  /** Contador: "e:{id}" empresa ou "p:{id}" pessoa física */
  const [tomadorChave, setTomadorChave] = useState("");
  const [incluirArquivadas, setIncluirArquivadas] = useState(false);
  const [payload, setPayload] = useState<PayloadValidados | null>(null);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");
  const [tabIdx, setTabIdx] = useState(0);

  const editavel = perfil === "CLIENTE" || perfil === "CONTADOR";

  const carregar = useCallback(async () => {
    setErro("");
    setLoading(true);
    try {
      let url = "/api/inteligencia/documentos/portal/validados-por-aba";
      if (perfil === "CONTADOR") {
        if (!tomadorChave) {
          setErro("Selecione o tomador (empresa ou pessoa física).");
          setPayload(null);
          setLoading(false);
          return;
        }
        const sep = tomadorChave.indexOf(":");
        const tipo = sep >= 0 ? tomadorChave.slice(0, sep) : "";
        const id = sep >= 0 ? tomadorChave.slice(sep + 1) : "";
        if ((tipo !== "e" && tipo !== "p") || !id) {
          setErro("Seleção de tomador inválida.");
          setPayload(null);
          setLoading(false);
          return;
        }
        const q =
          tipo === "e"
            ? `empresaId=${encodeURIComponent(id)}`
            : `clientePessoaFisicaId=${encodeURIComponent(id)}`;
        url += `?${q}&incluirCompetenciasArquivadas=${incluirArquivadas ? 1 : 0}`;
      }
      const data = await apiFetchJson<PayloadValidados>(url, { sessao, cache: "no-store" });
      setPayload(data);
      const abas = data.abas || [];
      const firstWithDocs = abas.findIndex((a) => (a.documentos || []).length > 0);
      setTabIdx(firstWithDocs >= 0 ? firstWithDocs : 0);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao carregar.");
      setPayload(null);
    } finally {
      setLoading(false);
    }
  }, [perfil, tomadorChave, incluirArquivadas, sessao]);

  useEffect(() => {
    if (perfil !== "CONTADOR") return;
    void (async () => {
      try {
        const [listEj, listPf] = await Promise.all([
          apiFetchJson<EmpresaOpt[]>("/api/empresas", { sessao }),
          apiFetchJson<ClientePfOpt[]>("/api/clientes-pessoa-fisica", { sessao }).catch(() => [] as ClientePfOpt[])
        ]);
        setEmpresas(listEj);
        setClientesPf(listPf);
        setTomadorChave((prev) => {
          if (prev) {
            return prev;
          }
          if (listEj.length > 0) {
            return `e:${listEj[0].id}`;
          }
          if (listPf.length > 0) {
            return `p:${listPf[0].id}`;
          }
          return "";
        });
      } catch {
        setEmpresas([]);
        setClientesPf([]);
      }
    })();
  }, [perfil, sessao]);

  useEffect(() => {
    if (perfil === "CONTADOR") {
      if (!tomadorChave) return;
    }
    void carregar();
  }, [perfil, tomadorChave, incluirArquivadas, carregar]);

  const abas = payload?.abas ?? [];
  const tabBtnRefs = useRef<(HTMLButtonElement | null)[]>([]);

  useLayoutEffect(() => {
    const el = tabBtnRefs.current[tabIdx];
    el?.scrollIntoView({ block: "nearest", inline: "center", behavior: "smooth" });
  }, [tabIdx, abas.length]);

  const cabecalho = useMemo(() => {
    if (perfil === "CONTADOR") {
      return (
        <div className="docs-validados-toolbar-react">
          <div className="docs-validados-toolbar-react__body">
            <div className="docs-validados-filters-react">
              <label className="docs-validados-field-react">
                <span className="docs-validados-field-react__lbl">Tomador</span>
                <select
                  value={tomadorChave}
                  onChange={(e) => setTomadorChave(e.target.value)}
                  disabled={!empresas.length && !clientesPf.length}
                  aria-label="Filtrar por empresa ou pessoa física"
                >
                  <option value="">Selecione...</option>
                  {empresas.map((e) => (
                    <option key={`e-${e.id}`} value={`e:${e.id}`}>
                      PJ: {e.razaoSocial} ({formatarCnpj(e.cnpj)})
                    </option>
                  ))}
                  {clientesPf.map((c) => (
                    <option key={`p-${c.id}`} value={`p:${c.id}`}>
                      PF: {c.nomeCompleto} ({formatarCpf(c.cpf)})
                    </option>
                  ))}
                </select>
              </label>
              <label className="check-react docs-validados-arq-check-react">
                <input
                  type="checkbox"
                  checked={incluirArquivadas}
                  onChange={(e) => setIncluirArquivadas(e.target.checked)}
                />
                Incluir competências arquivadas
              </label>
            </div>
          </div>
          <button type="button" className="btn-primary-react docs-validados-toolbar-react__btn" onClick={() => void carregar()}>
            Atualizar lista
          </button>
        </div>
      );
    }
    const cnpj = payload?.cnpj;
    const razao = payload?.razaoSocial;
    const cpf = payload?.cpfClientePf;
    const nomePf = payload?.nomeClientePf;
    if (cnpj && razao) {
      return (
        <p className="muted-react docs-validados-cliente-empresa-react" role="status">
          CNPJ: {formatarCnpj(cnpj)} — {razao}
        </p>
      );
    }
    if (cpf && nomePf) {
      return (
        <p className="muted-react docs-validados-cliente-empresa-react" role="status">
          CPF: {formatarCpf(cpf)} — {nomePf}
        </p>
      );
    }
    return (
      <p className="muted-react docs-validados-cliente-empresa-react" role="status">
        Nenhum tomador (empresa ou PF) vinculado ao seu usuário.
      </p>
    );
  }, [perfil, empresas, clientesPf, tomadorChave, incluirArquivadas, carregar, payload]);

  const totalAbas = abas.length;

  return (
    <section className="page page--elevated docs-validados-page-react">
      <header className="docs-validados-header-react">
        <h2 className="docs-validados-title-react">Documentos validados pela IA</h2>
        <p className="docs-validados-lead-react muted-react">
          Dados extraídos por tipo — use o carrossel abaixo para alternar a categoria.
        </p>
        <details className="docs-validados-help-react">
          <summary className="docs-validados-help-react__summary">Mais informações</summary>
          <p className="docs-validados-help-react__body muted-react">
            Holerites com captura completa exibem proventos, descontos e bases em destaque. No perfil contador, escolha o
            tomador (PJ ou PF) e marque <strong>competências arquivadas</strong> se quiser incluir meses já encerrados.
          </p>
        </details>
      </header>
      {cabecalho}
      {perfil === "CONTADOR" ? (
        <details className="docs-validados-arq-details-react muted-react">
          <summary className="docs-validados-arq-details-react__summary">O que são competências arquivadas?</summary>
          <p>
            É o mês em que <strong>todas</strong> as pendências foram validadas (mesma regra da tela Pendências). Com a
            opção desmarcada, esses meses não aparecem aqui.
          </p>
        </details>
      ) : null}
      {erro ? <p className="erro">{erro}</p> : null}
      {loading ? (
        <p className="muted-react docs-validados-loading-react">
          <span className="shell-spinner-react" aria-hidden="true" /> Carregando…
        </p>
      ) : null}

      {perfil === "CONTADOR" && empresas.length === 0 && clientesPf.length === 0 && !loading ? (
        <p className="muted-react">Cadastre uma empresa ou um cliente PF para visualizar os documentos validados.</p>
      ) : null}

      {payload && totalAbas > 0 ? (
        <>
          <div className="docs-validados-carousel-wrap">
            <div className="doc-tabs doc-tabs--scroll doc-tabs--carousel" role="tablist" aria-label="Tipos de documento">
              {abas.map((a, i) => {
                const n = (a.documentos || []).length;
                const idKey = idAbaFromPayload(a);
                return (
                  <button
                    key={`${a.idAba}-${i}`}
                    ref={(el) => {
                      tabBtnRefs.current[i] = el;
                    }}
                    type="button"
                    role="tab"
                    className={`doc-tab doc-tab--carousel ${i === tabIdx ? "doc-tab--active" : ""}`}
                    aria-selected={i === tabIdx}
                    title={a.titulo}
                    onClick={() => setTabIdx(i)}
                  >
                    <span className="doc-tab-carousel__icon-wrap">
                      <span className="doc-tab-carousel__icon">
                        <DocTabIconCarousel idAba={idKey} />
                      </span>
                      {n > 0 ? (
                        <span className="doc-tab-carousel__count" aria-label={`${n} documento(s)`}>
                          {n}
                        </span>
                      ) : null}
                    </span>
                    <span className="doc-tab-carousel__label">{a.titulo}</span>
                  </button>
                );
              })}
            </div>
            <nav className="doc-tabs-pager" aria-label="Navegação das categorias">
              <button
                type="button"
                className="doc-tabs-pager__btn"
                aria-label="Categoria anterior"
                disabled={tabIdx <= 0}
                onClick={() => setTabIdx((j) => Math.max(0, j - 1))}
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M15 18l-6-6 6-6" />
                </svg>
              </button>
              <span className="doc-tabs-pager__indicator" aria-live="polite">
                <span className="doc-tabs-pager__cur">{tabIdx + 1}</span>
                <span className="doc-tabs-pager__sep">/</span>
                <span className="doc-tabs-pager__tot">{totalAbas}</span>
              </span>
              <button
                type="button"
                className="doc-tabs-pager__btn"
                aria-label="Próxima categoria"
                disabled={tabIdx >= totalAbas - 1}
                onClick={() => setTabIdx((j) => Math.min(totalAbas - 1, j + 1))}
              >
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M9 18l6-6-6-6" />
                </svg>
              </button>
            </nav>
          </div>
          <div className="doc-tab-panels" role="tabpanel">
            {(() => {
              const a = abas[tabIdx];
              if (!a) return null;
              const docs = a.documentos || [];
              if (!docs.length) {
                return <p className="muted-react doc-tab-empty">Nenhum documento com análise concluída nesta categoria ainda.</p>;
              }
              return docs.map((d) => (
                <DocValidadoCard
                  key={d.processamentoId}
                  doc={d}
                  editavel={editavel}
                  sessao={sessao}
                  onSalvo={() => void carregar()}
                />
              ));
            })()}
          </div>
        </>
      ) : null}

      {!loading && payload && totalAbas === 0 ? <p className="muted-react">Nenhuma aba disponível.</p> : null}
    </section>
  );
}
