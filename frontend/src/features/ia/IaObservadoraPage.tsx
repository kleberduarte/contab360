import { useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type IaObservadoraResponse = {
  totalEventosPeriodo: number;
  acoesPorPerfil: Record<string, number>;
  acoesPorCategoria: Record<string, number>;
  topCaminhos: Record<string, number>;
  sugestoesAutonomia: string[];
  ultimosEventos: {
    criadoEm: string;
    usuarioEmail: string;
    perfil: string;
    metodoHttp: string;
    path: string;
    categoria: string;
    statusHttp: number | null;
  }[];
};

export function IaObservadoraPage({ sessao }: { sessao: Sessao }) {
  const [data, setData] = useState<IaObservadoraResponse | null>(null);
  const [erro, setErro] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void (async () => {
      setLoading(true);
      setErro("");
      try {
        const r = await apiFetchJson<IaObservadoraResponse>("/api/ia-observadora/insights", { sessao });
        setData(r);
      } catch (e) {
        setErro(e instanceof Error ? e.message : "Falha ao carregar.");
        setData(null);
      } finally {
        setLoading(false);
      }
    })();
  }, [sessao]);

  return (
    <section className="page ia-obs-page-react">
      <h2>IA Observadora</h2>
      <p className="muted-react small">Resumo de auditoria dos últimos 7 dias (apenas contador).</p>
      {loading ? <p className="muted-react">Carregando...</p> : null}
      {erro ? <p className="erro">{erro}</p> : null}
      {data ? (
        <>
          <div className="ia-obs-kpis-react">
            <div className="ia-obs-kpi-react">
              <strong>{data.totalEventosPeriodo}</strong>
              <span className="muted-react small">Eventos (amostra recente)</span>
            </div>
          </div>
          <div className="ia-obs-grid-react">
            <div>
              <h3>Por perfil</h3>
              <ul className="ia-obs-lista-react">
                {Object.entries(data.acoesPorPerfil).map(([k, v]) => (
                  <li key={k}>
                    {k}: <strong>{v}</strong>
                  </li>
                ))}
              </ul>
            </div>
            <div>
              <h3>Por categoria</h3>
              <ul className="ia-obs-lista-react">
                {Object.entries(data.acoesPorCategoria).map(([k, v]) => (
                  <li key={k}>
                    {k}: <strong>{v}</strong>
                  </li>
                ))}
              </ul>
            </div>
            <div className="ia-obs-span-2-react">
              <h3>Rotas mais frequentes</h3>
              <ul className="ia-obs-lista-react">
                {Object.entries(data.topCaminhos).map(([k, v]) => (
                  <li key={k}>
                    <code>{k}</code> — <strong>{v}</strong>
                  </li>
                ))}
              </ul>
            </div>
          </div>
          {data.sugestoesAutonomia.length ? (
            <div className="ia-obs-sugestoes-react">
              <h3>Sugestões</h3>
              <ul>
                {data.sugestoesAutonomia.map((s, i) => (
                  <li key={i}>{s}</li>
                ))}
              </ul>
            </div>
          ) : null}
          <div className="ia-obs-eventos-react">
            <h3>Últimos eventos</h3>
            <div className="ia-obs-table-wrap-react">
              <table className="empresas-react-table">
                <thead>
                  <tr>
                    <th>Quando</th>
                    <th>Usuário</th>
                    <th>Perfil</th>
                    <th>HTTP</th>
                    <th>Path</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {data.ultimosEventos.map((e, i) => (
                    <tr key={`${e.criadoEm}-${i}`}>
                      <td>{e.criadoEm}</td>
                      <td>{e.usuarioEmail}</td>
                      <td>{e.perfil}</td>
                      <td>{e.metodoHttp}</td>
                      <td>
                        <code>{e.path}</code>
                      </td>
                      <td>{e.statusHttp ?? "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : null}
    </section>
  );
}
