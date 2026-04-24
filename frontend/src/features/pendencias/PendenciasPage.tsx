import { FormEvent, useEffect, useMemo, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type Pendencia = {
  id: number;
  empresaId: number;
  empresaRazaoSocial: string;
  empresaCnpj: string;
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
  const [diaVencimento, setDiaVencimento] = useState(10);
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
      const result = await apiFetchJson<{ pendenciasCriadas: number }>("/api/pendencias/gerar", {
        method: "POST",
        body: JSON.stringify({ ano, mes, diaVencimento }),
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

  return (
    <section className="page page--pendencias pendencias-screen-react">
      <header className="pendencias-header">
        <h2>Pendências mensais</h2>
        <p className="pendencias-lead muted-react">Gere a competência, acompanhe por empresa e documento.</p>
      </header>

      <details className="pendencias-help">
        <summary className="pendencias-help__summary">Sobre arquivamento automático</summary>
        <p className="pendencias-help__body muted-react">
          Quando <strong>todas</strong> as pendências do mês estiverem <strong>validadas</strong>, a competência é{" "}
          <strong>arquivada</strong>: os dados permanecem no sistema e saem desta lista até você marcar “Incluir
          competências arquivadas”.
        </p>
      </details>

      <div className="pendencias-toolbar">
        <form id="form-gerar-pendencias" className="pendencias-form" onSubmit={onGerar}>
          <div className="pendencias-fields">
            <label className="pendencias-field">
              <span className="pendencias-field__lbl">Ano</span>
              <input
                type="number"
                className="pendencias-field__input"
                min={2000}
                max={2100}
                value={ano}
                onChange={(e) => setAno(Number(e.target.value))}
                required
              />
            </label>
            <label className="pendencias-field">
              <span className="pendencias-field__lbl">Mês</span>
              <input
                type="number"
                className="pendencias-field__input"
                min={1}
                max={12}
                value={mes}
                onChange={(e) => setMes(Number(e.target.value))}
                required
              />
            </label>
            <label className="pendencias-field">
              <span className="pendencias-field__lbl">Dia venc.</span>
              <input
                type="number"
                className="pendencias-field__input"
                min={1}
                max={31}
                value={diaVencimento}
                onChange={(e) => setDiaVencimento(Number(e.target.value))}
              />
            </label>
          </div>
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
          Incluir competências arquivadas (histórico)
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
                <p className="pendencia-card__title">{pendencia.empresaRazaoSocial}</p>
                <p className="pendencia-card__sub muted-react">
                  {formatarCnpj(pendencia.empresaCnpj)} · {pendencia.templateDocumentoNome}
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
