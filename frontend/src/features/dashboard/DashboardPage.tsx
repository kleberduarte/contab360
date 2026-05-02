import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";
import { DashboardDonutsCliente, DashboardDonutsContador } from "./DashboardDonuts";
import { PendenciaStatus } from "./dashboardTypes";

type Pendencia = {
  status: PendenciaStatus;
};

type RadarFiscal = {
  totalNotas: number;
  porTipoDocumento?: Record<string, number>;
  porOperacao?: Record<string, number>;
};

type RelatorioEstrategico = {
  totalNotasEmitidas: number;
  totalAlertasAbertos: number;
  totalCadastrosCpfCnpj: number;
  totalCobrancasGeradas: number;
  totalCertificadosVendidos: number;
};

function competenciaAtual() {
  const now = new Date();
  return { ano: now.getFullYear(), mes: now.getMonth() + 1 };
}

function competenciaAnterior(ano: number, mes: number) {
  if (mes > 1) return { ano, mes: mes - 1 };
  return { ano: ano - 1, mes: 12 };
}

function contarPendenciasPorStatus(list: Pendencia[]) {
  return list.reduce(
    (acc, p) => {
      if (p.status in acc) acc[p.status] += 1;
      return acc;
    },
    { PENDENTE: 0, ENVIADO: 0, VALIDADO: 0, REJEITADO: 0 } as Record<PendenciaStatus, number>
  );
}

function montarHintPendencias(st: Record<PendenciaStatus, number>) {
  return `Pendente ${st.PENDENTE} · Enviado ${st.ENVIADO} · Validado ${st.VALIDADO} · Rejeitado ${st.REJEITADO}`;
}

function resumoRadar(radar: RadarFiscal | null) {
  if (!radar || typeof radar.totalNotas !== "number") return "Indisponível.";
  const parts = [`${radar.totalNotas} nota(s) registrada(s)`];
  const porTipo = Object.entries(radar.porTipoDocumento || {}).filter(([, n]) => n > 0);
  if (porTipo.length) {
    parts.push(porTipo.map(([k, v]) => `${k}: ${v}`).join(" · "));
  }
  const porOp = Object.entries(radar.porOperacao || {}).filter(([, n]) => n > 0);
  if (porOp.length) {
    parts.push(porOp.map(([k, v]) => `${k}: ${v}`).join(" · "));
  }
  return `${parts.join(". ")}.`;
}

type Kpi = { label: string; value: string | number; hint?: string };

export function DashboardPage({ sessao }: { sessao: Sessao }) {
  const navigate = useNavigate();
  const comp = useMemo(() => competenciaAtual(), []);
  const [kpis, setKpis] = useState<Kpi[]>([]);
  const [radarTexto, setRadarTexto] = useState("Carregando...");
  const [radar, setRadar] = useState<RadarFiscal | null>(null);
  const [pendStats, setPendStats] = useState<Record<PendenciaStatus, number> | null>(null);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function carregar() {
      setLoading(true);
      setErro("");
      const errs: string[] = [];

      let pendList: Pendencia[] = [];
      let pendCompetencia = comp;
      let empresasN = 0;
      let iaN = 0;
      let rel: RelatorioEstrategico | null = null;
      let radarData: RadarFiscal | null = null;

      try {
        if (sessao.perfil === "CLIENTE") {
          let anoBusca = comp.ano;
          let mesBusca = comp.mes;
          for (let tentativa = 0; tentativa < 6; tentativa++) {
            const data = await apiFetchJson<Pendencia[]>(
              `/api/pendencias?ano=${anoBusca}&mes=${mesBusca}&incluirArquivadas=true`,
              { sessao }
            );
            pendList = data;
            pendCompetencia = { ano: anoBusca, mes: mesBusca };
            if (data.length > 0) {
              break;
            }
            const anterior = competenciaAnterior(anoBusca, mesBusca);
            anoBusca = anterior.ano;
            mesBusca = anterior.mes;
          }
        } else {
          pendList = await apiFetchJson<Pendencia[]>(
            `/api/pendencias?ano=${comp.ano}&mes=${comp.mes}&incluirArquivadas=true`,
            { sessao }
          );
        }
      } catch {
        errs.push("pendências");
      }

      const st = contarPendenciasPorStatus(pendList);
      setPendStats(st);
      const pendHint = montarHintPendencias(st);

      if (sessao.perfil === "CONTADOR") {
        try {
          const empresas = await apiFetchJson<unknown[]>("/api/empresas", { sessao });
          empresasN = empresas.length;
        } catch {
          errs.push("empresas");
        }
        try {
          rel = await apiFetchJson<RelatorioEstrategico>("/api/fiscal/relatorios/estrategico", { sessao });
        } catch {
          errs.push("relatório fiscal");
        }
        try {
          radarData = await apiFetchJson<RadarFiscal>("/api/fiscal/radar", { sessao });
          setRadar(radarData);
        } catch {
          errs.push("radar de notas");
          setRadar(null);
        }
        try {
          const fila = await apiFetchJson<unknown[]>(
            "/api/inteligencia/documentos?somenteRevisar=true&incluirConcluidosNaRevisao=false",
            { sessao }
          );
          iaN = fila.length;
        } catch {
          errs.push("fila IA");
        }

        setKpis([
          { value: empresasN, label: "Empresas cadastradas" },
          { value: pendList.length, label: "Pendências no mês", hint: pendHint },
          { value: iaN, label: "Fila revisão IA" },
          { value: rel ? rel.totalNotasEmitidas : "—", label: "Notas fiscais (total)" },
          { value: rel ? rel.totalAlertasAbertos : "—", label: "Alertas em aberto" },
          { value: rel ? rel.totalCadastrosCpfCnpj : "—", label: "Cadastros CPF/CNPJ" },
          { value: rel ? rel.totalCobrancasGeradas : "—", label: "Cobranças" },
          { value: rel ? rel.totalCertificadosVendidos : "—", label: "Certificados (pedidos)" }
        ]);
        setRadarTexto(resumoRadar(radarData));
      } else {
        setRadar(null);
        const emAberto = st.PENDENTE + st.ENVIADO;
        const houveFallback =
          pendList.length > 0 && (pendCompetencia.ano !== comp.ano || pendCompetencia.mes !== comp.mes);
        const competenciaTexto = `${String(pendCompetencia.mes).padStart(2, "0")}/${pendCompetencia.ano}`;
        const labelPendencias = houveFallback ? "Pendências da competência exibida" : "Pendências no mês";
        const hintPendencias = houveFallback ? `Competência exibida: ${competenciaTexto}.` : pendHint;
        const hintEmAberto = houveFallback
          ? `Itens da competência ${competenciaTexto} que ainda exigem ação ou análise.`
          : "Itens a concluir ou em análise";
        setKpis([
          { value: pendList.length, label: labelPendencias, hint: hintPendencias },
          { value: emAberto, label: "Em aberto (pendente + enviado)", hint: hintEmAberto }
        ]);
        setRadarTexto("Visão fiscal disponível no perfil contador.");
      }

      if (errs.length) {
        setErro(`Não foi possível atualizar: ${errs.join(", ")}.`);
      }
      setLoading(false);
    }

    void carregar();
  }, [comp.ano, comp.mes, sessao]);

  const acoes =
    sessao.perfil === "CONTADOR"
      ? [
          { label: "Pendências", to: "/pendencias" },
          { label: "Revisão IA", to: "/ia-revisao" },
          { label: "Empresas", to: "/empresas" },
          { label: "Notas fiscais", to: "/fiscal-notas" },
          { label: "Alertas e relatórios", to: "/fiscal-alertas" }
        ]
      : [
          { label: "Minhas pendências", to: "/cliente-pendencias" },
          { label: "Enviar documento", to: "/cliente-upload" },
          { label: "Documentos validados (IA)", to: "/docs-validados" }
        ];

  const compBadge = `${String(comp.mes).padStart(2, "0")}/${comp.ano}`;
  const eyebrow =
    sessao.perfil === "CONTADOR" ? "Contab360 · Contador" : "Contab360 · Portal";
  const title = sessao.perfil === "CONTADOR" ? "Painel do contador" : "Portal do cliente";

  return (
    <section className="page page--dashboard dashboard-page-react">
      <header className="dashboard-hero">
        <div className="dashboard-hero__inner">
          <p className="dashboard-hero__eyebrow">{eyebrow}</p>
          <h2 className="dashboard-hero__title">{title}</h2>
          <p className="dashboard-hero__lead">
            {sessao.perfil === "CONTADOR"
              ? "Visão geral da operação, indicadores e atalhos do período."
              : "Acompanhe suas pendências e envios do período."}
            <span className="dashboard-hero__comp">
              <span className="dashboard-hero__comp-label">Competência</span>
              <span className="dashboard-badge">{compBadge}</span>
            </span>
          </p>
          {sessao.perfil === "CONTADOR" ? (
            <p className="dashboard-hero__note muted-react">
              Os números de pendências refletem a competência do mês (inclui competências já arquivadas quando tudo foi
              validado). O gráfico <strong>Notas fiscais (por tipo)</strong> usa o cadastro do módulo fiscal (radar de
              notas), não só arquivos validados na IA.
            </p>
          ) : (
            <p className="dashboard-hero__note muted-react">
              Os totais de pendências consideram a competência atual e histórico quando aplicável.
            </p>
          )}
        </div>
      </header>

      <div className="dashboard-body">
        {erro ? (
          <p className="feedback-react erro" role="status">
            {erro}
          </p>
        ) : null}

        {sessao.perfil === "CONTADOR" ? (
          <DashboardDonutsContador st={pendStats} radar={radar} loading={loading} />
        ) : (
          <DashboardDonutsCliente st={pendStats} loading={loading} />
        )}

        <div className="dashboard-kpis" aria-live="polite">
          {kpis.map((kpi) => (
            <article key={kpi.label} className="dashboard-kpi">
              <span className="dashboard-kpi__value">{kpi.value}</span>
              <span className="dashboard-kpi__label">{kpi.label}</span>
              {kpi.hint ? <span className="dashboard-kpi__hint">{kpi.hint}</span> : null}
            </article>
          ))}
        </div>

        <div className="dashboard-panels dashboard-panels--cols">
          <div className="dashboard-panel">
            <h3 className="dashboard-h3">Notas fiscais</h3>
            <p className="dashboard-radar muted-react">{radarTexto}</p>
          </div>
          <div className="dashboard-panel dashboard-panel--actions">
            <h3 className="dashboard-h3">Atalhos</h3>
            <div className="dashboard-actions">
              {acoes.map((acao) => (
                <button key={acao.to} type="button" className="btn-dashboard-ghost" onClick={() => navigate(acao.to)}>
                  {acao.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
