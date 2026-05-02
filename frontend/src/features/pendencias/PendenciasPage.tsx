import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";
import { CompetenciaMonthPicker } from "./CompetenciaMonthPicker";

type EmpresaOpt = { id: number; razaoSocial: string; cnpj: string; ativo?: boolean };
type ClientePfOpt = { id: number; nomeCompleto: string; cpf: string; ativo?: boolean };
type TemplateOpt = { id: number; nome: string; obrigatorio?: boolean };

type ModoGeracao = "todos" | "cliente" | "individual";
const DIA_VENCIMENTO_PADRAO = 10;

const OPCOES_MODO: { id: ModoGeracao; label: string; dica: string }[] = [
  {
    id: "todos",
    label: "Toda a carteira",
    dica: "Todos os PJ e PF ativos, com a mesma competência."
  },
  {
    id: "cliente",
    label: "Um tomador — todos os documentos",
    dica: "Só o tomador que você escolher; entram todos os tipos de documento dele."
  },
  {
    id: "individual",
    label: "Um tomador — um documento",
    dica: "Um tomador e um único tipo de documento (ex.: nota fiscal, guia)."
  }
];

function ativaEmpresa(e: EmpresaOpt): boolean {
  return e.ativo !== false;
}
function ativoPf(c: ClientePfOpt): boolean {
  return c.ativo !== false;
}

type Pendencia = {
  id: number;
  empresaId: number | null;
  empresaRazaoSocial: string | null;
  empresaCnpj: string | null;
  clientePessoaFisicaId: number | null;
  clientePessoaFisicaNome: string | null;
  clientePessoaFisicaCpf: string | null;
  templateDocumentoId: number;
  templateDocumentoNome: string;
  competenciaAno: number;
  competenciaMes: number;
  status: "PENDENTE" | "ENVIADO" | "VALIDADO" | "REJEITADO";
  vencimento: string;
  observacaoAnalise: string | null;
};

type CompetenciaArquivoInfo = {
  existeCompetencia: boolean;
  arquivada: boolean;
  arquivadaEm: string | null;
};

function hojeCompetencia() {
  const now = new Date();
  return { ano: now.getFullYear(), mes: now.getMonth() + 1 };
}

function formatarCnpj(value: string): string {
  const d = (value || "").replace(/\D/g, "").slice(0, 14);
  return d
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1/$2")
    .replace(/(\d{4})(\d)/, "$1-$2");
}

function formatarCpf(value: string): string {
  const d = (value || "").replace(/\D/g, "").slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

function badgeStatus(status: Pendencia["status"]) {
  if (status === "VALIDADO") return { cls: "status-ok", label: "Validado" };
  if (status === "ENVIADO") return { cls: "status-analise", label: "Enviado" };
  if (status === "REJEITADO") return { cls: "status-rejeitado", label: "Rejeitado" };
  return { cls: "status-pendente", label: "Pendente" };
}

export function PendenciasPage({ sessao }: { sessao: Sessao }) {
  const comp = useMemo(() => hojeCompetencia(), []);
  const [ano, setAno] = useState(comp.ano);
  const [mes, setMes] = useState(comp.mes);
  const anoCalendarioRef = useRef(new Date().getFullYear());
  const [modoGeracao, setModoGeracao] = useState<ModoGeracao>("todos");
  const [diaVencimento, setDiaVencimento] = useState(DIA_VENCIMENTO_PADRAO);
  const [competenciaCustomizada, setCompetenciaCustomizada] = useState(false);
  const [vencimentoCustomizado, setVencimentoCustomizado] = useState(false);
  const [tomadorTipo, setTomadorTipo] = useState<"PJ" | "PF">("PJ");
  const [tomadorId, setTomadorId] = useState("");
  const [templateDocumentoId, setTemplateDocumentoId] = useState("");
  const [empresas, setEmpresas] = useState<EmpresaOpt[]>([]);
  const [clientesPf, setClientesPf] = useState<ClientePfOpt[]>([]);
  const [templatesTomador, setTemplatesTomador] = useState<TemplateOpt[]>([]);
  const [loadingTomadores, setLoadingTomadores] = useState(false);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [incluirArquivadas, setIncluirArquivadas] = useState(false);
  const [pendencias, setPendencias] = useState<Pendencia[]>([]);
  const [arquivoInfo, setArquivoInfo] = useState<CompetenciaArquivoInfo>({
    existeCompetencia: false,
    arquivada: false,
    arquivadaEm: null
  });
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");
  const temAjusteManual = competenciaCustomizada || vencimentoCustomizado;

  const carregarTomadores = useCallback(async () => {
    setLoadingTomadores(true);
    try {
      const [e, p] = await Promise.all([
        apiFetchJson<EmpresaOpt[]>("/api/empresas", { sessao }),
        apiFetchJson<ClientePfOpt[]>("/api/clientes-pessoa-fisica", { sessao }).catch(() => [] as ClientePfOpt[])
      ]);
      setEmpresas((e || []).filter(ativaEmpresa));
      setClientesPf((p || []).filter(ativoPf));
    } catch {
      setEmpresas([]);
      setClientesPf([]);
    } finally {
      setLoadingTomadores(false);
    }
  }, [sessao]);

  useEffect(() => {
    void carregarTomadores();
  }, [carregarTomadores]);

  useEffect(() => {
    setTemplateDocumentoId("");
    setTemplatesTomador([]);
    if (modoGeracao !== "individual" || !tomadorId) {
      return;
    }
    let cancelled = false;
    setLoadingTemplates(true);
    const qs =
      tomadorTipo === "PJ"
        ? `empresaId=${encodeURIComponent(tomadorId)}`
        : `clientePessoaFisicaId=${encodeURIComponent(tomadorId)}`;
    void apiFetchJson<TemplateOpt[]>(`/api/templates-documentos?${qs}`, { sessao })
      .then((data) => {
        if (cancelled) return;
        setTemplatesTomador(
          (data || []).map((t) => ({
            id: t.id,
            nome: t.nome ?? ""
          }))
        );
      })
      .catch(() => {
        if (!cancelled) setTemplatesTomador([]);
      })
      .finally(() => {
        if (!cancelled) setLoadingTemplates(false);
      });
    return () => {
      cancelled = true;
    };
  }, [modoGeracao, tomadorTipo, tomadorId, sessao]);

  useEffect(() => {
    if (modoGeracao === "todos") {
      setTomadorId("");
      setTemplateDocumentoId("");
    }
  }, [modoGeracao]);

  async function carregarPendencias() {
    setLoading(true);
    setErro("");
    try {
      const info = await apiFetchJson<CompetenciaArquivoInfo>(
        `/api/pendencias/competencia-arquivo?ano=${ano}&mes=${mes}`,
        { sessao }
      );
      setArquivoInfo(info);
    } catch {
      setArquivoInfo({ existeCompetencia: false, arquivada: false, arquivadaEm: null });
    }

    try {
      const data = await apiFetchJson<Pendencia[]>(
        `/api/pendencias?ano=${ano}&mes=${mes}&incluirArquivadas=${incluirArquivadas}`,
        { sessao }
      );
      setPendencias(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao listar pendências.");
      setPendencias([]);
    } finally {
      setLoading(false);
    }
  }

  async function onGerar(ev: FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    setErro("");
    setOk("");
    try {
      let empresaId: number | undefined;
      let clientePessoaFisicaId: number | undefined;
      let templateId: number | undefined;

      if (modoGeracao === "cliente" || modoGeracao === "individual") {
        if (!tomadorId) {
          setErro("Selecione o tomador (empresa ou cliente PF) acima.");
          return;
        }
        const idNum = Number(tomadorId);
        if (tomadorTipo === "PJ") {
          empresaId = idNum;
        } else {
          clientePessoaFisicaId = idNum;
        }
      }

      if (modoGeracao === "individual") {
        if (!templateDocumentoId) {
          setErro("Selecione o tipo de documento no último campo.");
          return;
        }
        templateId = Number(templateDocumentoId);
      }

      const body: Record<string, unknown> = { ano, mes, diaVencimento };
      if (empresaId != null) body.empresaId = empresaId;
      if (clientePessoaFisicaId != null) body.clientePessoaFisicaId = clientePessoaFisicaId;
      if (templateId != null) body.templateDocumentoId = templateId;

      const result = await apiFetchJson<{ pendenciasCriadas: number }>("/api/pendencias/gerar", {
        method: "POST",
        body: JSON.stringify(body),
        sessao
      });
      setOk(`${result.pendenciasCriadas} pendência(s) criada(s).`);
      await carregarPendencias();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao gerar pendências.");
    }
  }

  useEffect(() => {
    void carregarPendencias();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ano, mes, incluirArquivadas]);

  useEffect(() => {
    const sincronizarSeVirouAno = () => {
      const y = new Date().getFullYear();
      if (y !== anoCalendarioRef.current) {
        anoCalendarioRef.current = y;
        const vigente = hojeCompetencia();
        setAno(vigente.ano);
        setMes(vigente.mes);
        setCompetenciaCustomizada(false);
      }
    };
    const id = window.setInterval(sincronizarSeVirouAno, 60_000);
    const onVis = () => {
      if (document.visibilityState === "visible") sincronizarSeVirouAno();
    };
    document.addEventListener("visibilitychange", onVis);
    return () => {
      window.clearInterval(id);
      document.removeEventListener("visibilitychange", onVis);
    };
  }, []);

  return (
    <section className="page page--pendencias pendencias-screen-react">
      <header className="pendencias-header">
        <h2>Pendências mensais</h2>
        <p className="pendencias-lead muted-react">
          Escolha a competência e quem entra na geração; a lista abaixo mostra o que já existe para o mês.
        </p>
      </header>

      <details className="pendencias-help">
        <summary className="pendencias-help__summary">Sobre arquivamento automático</summary>
        <p className="pendencias-help__body muted-react">
          Quando <strong>todas</strong> as pendências do mês estiverem <strong>validadas</strong>, a competência é{" "}
          <strong>arquivada</strong>: os dados permanecem no sistema e saem desta lista até você marcar “Ver também
          competências arquivadas”.
        </p>
      </details>

      <div className="pendencias-toolbar">
        <form id="form-gerar-pendencias" className="pendencias-form" onSubmit={onGerar}>
          <div className="pendencias-form__group pendencias-form__group--competencia">
            <div className="pendencias-competencia-card">
              <div className="pendencias-competencia-card__head">
                <span className="pendencias-competencia-card__icon" aria-hidden="true">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path
                      d="M8 2v3M16 2v3M3.5 9.09h17M21 8.5V17a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8.5a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3Z"
                      stroke="currentColor"
                      strokeWidth="1.75"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </span>
                <span className="pendencias-competencia-card__title">Competência</span>
              </div>
              <div className="pendencias-competencia-row">
                <div className="pendencias-field pendencias-field--month">
                  <span className="pendencias-field__lbl pendencias-field__lbl--soft" id="pend-competencia-mes-ano">
                    Mês e ano
                  </span>
                  <CompetenciaMonthPicker
                    ariaLabelledBy="pend-competencia-mes-ano"
                    ano={ano}
                    mes={mes}
                    onChange={(a, m) => {
                      setAno(a);
                      setMes(m);
                      setCompetenciaCustomizada(a !== comp.ano || m !== comp.mes);
                    }}
                  />
                </div>
                <label className="pendencias-field">
                  <span className="pendencias-field__lbl pendencias-field__lbl--soft">Dia do vencimento</span>
                  <input
                    type="number"
                    className="pendencias-field__input"
                    min={1}
                    max={31}
                    value={diaVencimento}
                    onChange={(e) => {
                      const valor = Number(e.target.value);
                      if (Number.isNaN(valor)) return;
                      const valorClamped = Math.min(31, Math.max(1, valor));
                      setDiaVencimento(valorClamped);
                      setVencimentoCustomizado(valorClamped !== DIA_VENCIMENTO_PADRAO);
                    }}
                    required
                  />
                </label>
              </div>
              <div className="pendencias-competencia-meta" role="status" aria-live="polite">
                {temAjusteManual ? (
                  <>
                    <span className="pendencias-competencia-flag">Alterado manualmente</span>
                    <button
                      type="button"
                      className="pendencias-competencia-reset"
                      onClick={() => {
                        setAno(comp.ano);
                        setMes(comp.mes);
                        setDiaVencimento(DIA_VENCIMENTO_PADRAO);
                        setCompetenciaCustomizada(false);
                        setVencimentoCustomizado(false);
                      }}
                    >
                      Voltar ao padrão automático
                    </button>
                  </>
                ) : null}
              </div>
            </div>
          </div>

          <div className="pendencias-form__group">
            <span className="pendencias-form__group-label">Gerar para</span>
            <select
              className="pendencias-field__input pendencias-field__input--escopo"
              value={modoGeracao}
              onChange={(e) => setModoGeracao(e.target.value as ModoGeracao)}
              aria-label="Escopo da geração"
              aria-describedby="pend-escopo-dica"
            >
                {OPCOES_MODO.map((op) => (
                  <option key={op.id} value={op.id}>
                    {op.label}
                  </option>
                ))}
            </select>
            <p id="pend-escopo-dica" className="pendencias-escopo-hint muted-react" role="status">
              {OPCOES_MODO.find((o) => o.id === modoGeracao)?.dica ?? ""}
            </p>
          </div>

          {modoGeracao !== "todos" ? (
            <div className="pendencias-tomador-panel">
              <div className="pendencias-tomador-panel__head">
                <span className="pendencias-tomador-panel__title">Tomador</span>
                <span className="pendencias-tomador-panel__sub muted-react">Empresa (PJ) ou pessoa física (PF).</span>
              </div>
              <div className="pendencias-tomador-panel__row">
                <div className="pendencias-segment" role="group" aria-label="Tipo de tomador">
                  <button
                    type="button"
                    className={`pendencias-segment__btn${tomadorTipo === "PJ" ? " pendencias-segment__btn--on" : ""}`}
                    onClick={() => {
                      setTomadorTipo("PJ");
                      setTomadorId("");
                      setTemplateDocumentoId("");
                    }}
                  >
                    Empresa · PJ
                  </button>
                  <button
                    type="button"
                    className={`pendencias-segment__btn${tomadorTipo === "PF" ? " pendencias-segment__btn--on" : ""}`}
                    onClick={() => {
                      setTomadorTipo("PF");
                      setTomadorId("");
                      setTemplateDocumentoId("");
                    }}
                  >
                    Pessoa · PF
                  </button>
                </div>
                <label className="pendencias-field pendencias-field--grow pendencias-field--panel">
                  <span className="pendencias-field__lbl">
                    {tomadorTipo === "PJ" ? "Empresa" : "Cliente PF"}
                  </span>
                  <select
                    className="pendencias-field__input pendencias-field__input--panel"
                    value={tomadorId}
                    onChange={(e) => {
                      setTomadorId(e.target.value);
                      setTemplateDocumentoId("");
                    }}
                    disabled={loadingTomadores}
                  >
                    <option value="">{loadingTomadores ? "Carregando…" : "Selecione na lista…"}</option>
                    {tomadorTipo === "PJ"
                      ? empresas.map((em) => (
                          <option key={em.id} value={em.id}>
                            {em.razaoSocial}
                          </option>
                        ))
                      : clientesPf.map((c) => (
                          <option key={c.id} value={c.id}>
                            {c.nomeCompleto}
                          </option>
                        ))}
                  </select>
                </label>
                {modoGeracao === "individual" ? (
                  <label className="pendencias-field pendencias-field--grow pendencias-field--panel">
                    <span className="pendencias-field__lbl">Tipo de documento</span>
                    <select
                      className="pendencias-field__input pendencias-field__input--panel"
                      value={templateDocumentoId}
                      onChange={(e) => setTemplateDocumentoId(e.target.value)}
                      disabled={!tomadorId || loadingTemplates}
                    >
                      <option value="">
                        {!tomadorId
                          ? "Primeiro escolha quem acima"
                          : loadingTemplates
                            ? "Carregando…"
                            : templatesTomador.length === 0
                              ? "Nenhum documento cadastrado para este tomador"
                              : "Qual documento?"}
                      </option>
                      {templatesTomador.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.nome}
                        </option>
                      ))}
                    </select>
                  </label>
                ) : null}
              </div>
            </div>
          ) : null}
          <div className="pendencias-actions">
            <button type="submit" className="pendencias-btn-primary btn-primary-react">
              Gerar pendências
            </button>
            <button type="button" className="pendencias-btn-secondary ghost" onClick={() => void carregarPendencias()}>
              Atualizar lista
            </button>
          </div>
        </form>
        <label className="pend-arquivadas-check">
          <input
            type="checkbox"
            checked={incluirArquivadas}
            onChange={(e) => setIncluirArquivadas(e.target.checked)}
          />
          Ver também competências arquivadas
        </label>
      </div>

      {arquivoInfo.arquivada && incluirArquivadas && pendencias.length ? (
        <p className="pend-arquivo-banner">
          Competência arquivada
          {arquivoInfo.arquivadaEm ? ` em ${new Date(arquivoInfo.arquivadaEm).toLocaleString("pt-BR")}` : ""} — todos os
          itens estão validados.
        </p>
      ) : null}

      {ok ? <p className="ok">{ok}</p> : null}
      {erro ? <p className="erro">{erro}</p> : null}
      {loading ? (
        <p className="muted-react pendencias-loading-react">
          <span className="shell-spinner-react" aria-hidden="true" /> Carregando…
        </p>
      ) : null}
      {!loading && pendencias.length === 0 ? (
        <div className="pendencias-empty muted-react">Nenhuma pendência para a competência.</div>
      ) : null}

      <ul className="pendencias-grid">
        {pendencias.map((pendencia) => {
          const badge = badgeStatus(pendencia.status);
          return (
            <li key={pendencia.id}>
              <article className="pendencia-card">
                <div className="pendencia-card__row">
                  <span className="pendencia-card__id">#{pendencia.id}</span>
                  <span className={`status-chip ${badge.cls}`}>{badge.label}</span>
                </div>
                <p className="pendencia-card__title">
                  {pendencia.empresaRazaoSocial || pendencia.clientePessoaFisicaNome || "—"}
                </p>
                <p className="pendencia-card__sub muted-react">
                  {pendencia.empresaCnpj
                    ? formatarCnpj(pendencia.empresaCnpj)
                    : pendencia.clientePessoaFisicaCpf
                      ? formatarCpf(pendencia.clientePessoaFisicaCpf)
                      : "—"}{" "}
                  · {pendencia.templateDocumentoNome}
                </p>
                <div className="pendencia-card__meta">
                  <span>
                    Competência: {String(pendencia.competenciaMes).padStart(2, "0")}/{pendencia.competenciaAno}
                  </span>
                  <span>Vencimento: {pendencia.vencimento || "—"}</span>
                </div>
                {pendencia.observacaoAnalise ? (
                  <p className="pendencia-card__obs muted-react">Observação IA: {pendencia.observacaoAnalise}</p>
                ) : null}
              </article>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
