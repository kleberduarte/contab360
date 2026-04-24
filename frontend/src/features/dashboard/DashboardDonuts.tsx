import { PendenciaStatus } from "./dashboardTypes";

export type Segment = { label: string; value: number; color: string };

const DONUT_COLORS = {
  accent: "#2563EB",
  green: "#10B981",
  amber: "#F59E0B",
  red: "#EF4444",
  slate: "#94A3B8",
  violet: "#8B5CF6"
};

const LABEL_DOC_FISCAL: Record<string, string> = {
  NFE: "NF-e",
  NFSE: "NFS-e",
  NFCE: "NFC-e",
  CTE: "CT-e",
  MDFE: "MDF-e",
  CTE_OS: "CT-e OS"
};

function conicGradientFromSegments(segments: Segment[]): string {
  const total = segments.reduce((s, x) => s + x.value, 0);
  if (total <= 0) {
    return `conic-gradient(${DONUT_COLORS.slate} 0deg 360deg)`;
  }
  let acc = 0;
  const parts: string[] = [];
  segments.forEach((it) => {
    const frac = it.value / total;
    const start = acc;
    acc += frac;
    const a0 = (start * 360).toFixed(3);
    const a1 = (acc * 360).toFixed(3);
    parts.push(`${it.color} ${a0}deg ${a1}deg`);
  });
  return `conic-gradient(${parts.join(", ")})`;
}

function DonutCard({
  title,
  segments,
  centerText
}: {
  title: string;
  segments: Segment[];
  centerText: string;
}) {
  const grad = conicGradientFromSegments(segments);
  const total = segments.reduce((s, x) => s + x.value, 0);
  const center =
    centerText !== undefined && centerText !== null && centerText !== ""
      ? centerText
      : total === 0
        ? "—"
        : String(total);

  return (
    <div className="dashboard-donut-card">
      <h4 className="dashboard-donut-card__title">{title}</h4>
      <div className="dashboard-donut-visual">
        <div className="dashboard-donut-ring">
          <div className="dashboard-donut-fill" style={{ background: grad }} />
          <div className="dashboard-donut-hole">{center}</div>
        </div>
        <ul className="dashboard-donut-legend">
          {segments.map((it) => (
            <li key={it.label}>
              <span className="dashboard-donut-swatch" style={{ background: it.color }} />
              {it.label}
              <span className="dashboard-donut-legend__val">{it.value}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export function DashboardDonutsContador({
  st,
  radar,
  loading
}: {
  st: Record<PendenciaStatus, number> | null;
  radar: {
    totalNotas: number;
    porTipoDocumento?: Record<string, number>;
  } | null;
  loading: boolean;
}) {
  if (loading) {
    return (
      <div className="dashboard-donuts-loading">
        <span className="shell-spinner-react" aria-hidden="true" />
        <span className="muted-react">Carregando visão 360°…</span>
      </div>
    );
  }

  const pendSeg: Segment[] = [
    { label: "Pendente", value: st?.PENDENTE ?? 0, color: DONUT_COLORS.amber },
    { label: "Enviado", value: st?.ENVIADO ?? 0, color: DONUT_COLORS.accent },
    { label: "Validado", value: st?.VALIDADO ?? 0, color: DONUT_COLORS.green },
    { label: "Rejeitado", value: st?.REJEITADO ?? 0, color: DONUT_COLORS.red }
  ];
  const totalPend = pendSeg.reduce((s, x) => s + x.value, 0);

  const porTipo = radar?.porTipoDocumento || {};
  const tipoEntries = Object.entries(porTipo).filter(([, n]) => n > 0);
  const palette = [
    DONUT_COLORS.accent,
    DONUT_COLORS.green,
    DONUT_COLORS.amber,
    DONUT_COLORS.violet,
    DONUT_COLORS.red,
    DONUT_COLORS.slate
  ];
  let radarSeg: Segment[] = tipoEntries.map(([k, n], i) => ({
    label: LABEL_DOC_FISCAL[k] || k,
    value: n,
    color: palette[i % palette.length]
  }));
  const totalRadar =
    radar && typeof radar.totalNotas === "number"
      ? radar.totalNotas
      : tipoEntries.reduce((s, [, n]) => s + n, 0);
  if (radarSeg.length === 0) {
    radarSeg = [{ label: "Nenhuma nota por tipo", value: 0, color: DONUT_COLORS.slate }];
  }

  return (
    <div className="dashboard-donuts-wrap" aria-label="Resumo visual 360°">
      <div className="dashboard-donuts">
        <DonutCard title="Pendências do mês (status)" segments={pendSeg} centerText={String(totalPend)} />
        <DonutCard title="Notas fiscais (por tipo)" segments={radarSeg} centerText={String(totalRadar)} />
      </div>
    </div>
  );
}

export function DashboardDonutsCliente({
  st,
  loading
}: {
  st: Record<PendenciaStatus, number> | null;
  loading: boolean;
}) {
  if (loading) {
    return (
      <div className="dashboard-donuts-loading">
        <span className="shell-spinner-react" aria-hidden="true" />
        <span className="muted-react">Carregando visão 360°…</span>
      </div>
    );
  }

  const pendSeg: Segment[] = [
    { label: "Pendente", value: st?.PENDENTE ?? 0, color: DONUT_COLORS.amber },
    { label: "Enviado", value: st?.ENVIADO ?? 0, color: DONUT_COLORS.accent },
    { label: "Validado", value: st?.VALIDADO ?? 0, color: DONUT_COLORS.green },
    { label: "Rejeitado", value: st?.REJEITADO ?? 0, color: DONUT_COLORS.red }
  ];
  const totalPend = pendSeg.reduce((s, x) => s + x.value, 0);

  return (
    <div className="dashboard-donuts-wrap" aria-label="Resumo visual">
      <div className="dashboard-donuts">
        <DonutCard title="Suas pendências (status)" segments={pendSeg} centerText={String(totalPend)} />
      </div>
    </div>
  );
}
