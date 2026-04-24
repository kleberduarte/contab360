import { useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type Item = {
  id: number;
  pendenciaId: number | null;
  nomeArquivoOriginal: string;
  status: string;
  severidade: string;
  tipoDocumento: string;
  confianca: number | null;
  observacaoProcessamento: string | null;
  dadosExtraidosJson: string | null;
};

type HistoricoLinha = { acao: string; motivo: string | null; usuarioNome: string; criadoEm: string };

export function IaRevisaoPage({ sessao }: { sessao: Sessao }) {
  const [incluirConcluidos, setIncluirConcluidos] = useState(false);
  const [itens, setItens] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState("");
  const [historico, setHistorico] = useState<HistoricoLinha[] | null>(null);
  const [histProcId, setHistProcId] = useState<number | null>(null);
  const [dadosPendencia, setDadosPendencia] = useState<string | null>(null);

  async function carregar() {
    setLoading(true);
    setErro("");
    try {
      const data = await apiFetchJson<Item[]>(
        `/api/inteligencia/documentos?somenteRevisar=true&incluirConcluidosNaRevisao=${incluirConcluidos}`,
        { sessao }
      );
      setItens(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar.");
      setItens([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void carregar();
  }, [incluirConcluidos, sessao]);

  async function abrirHistorico(processamentoId: number) {
    setHistProcId(processamentoId);
    try {
      const h = await apiFetchJson<HistoricoLinha[]>(`/api/inteligencia/documentos/${processamentoId}/historico`, {
        sessao
      });
      setHistorico(h);
    } catch {
      setHistorico([]);
    }
  }

  return (
    <section className="page">
      <h2>Revisão de documentos (IA)</h2>
      <p className="muted-react small">Itens pendentes de revisão; opcionalmente inclua concluídos.</p>
      <label className="check-react" style={{ marginBottom: "0.75rem" }}>
        <input
          type="checkbox"
          checked={incluirConcluidos}
          onChange={(e) => setIncluirConcluidos(e.target.checked)}
        />
        Exibir concluídos (validados / mês arquivado)
      </label>
      {erro ? <p className="erro">{erro}</p> : null}
      {loading ? <p className="muted-react">Carregando...</p> : null}
      <ul className="ia-revisao-lista">
        {itens.map((item) => (
          <li key={item.id} className="ia-revisao-item">
            <strong>
              #{item.id} — {item.nomeArquivoOriginal}
            </strong>
            <span className="muted-react small">
              {" "}
              {item.tipoDocumento} · conf. {item.confianca != null ? item.confianca.toFixed(2) : "—"} · {item.status}{" "}
              <span className="severity-pill">{item.severidade}</span>
            </span>
            {item.observacaoProcessamento ? (
              <p className="muted-react small">{item.observacaoProcessamento}</p>
            ) : null}
            <details className="ia-revisao-json">
              <summary>JSON / dados extraídos</summary>
              <pre>
                {item.dadosExtraidosJson
                  ? (() => {
                      try {
                        return JSON.stringify(JSON.parse(item.dadosExtraidosJson), null, 2);
                      } catch {
                        return item.dadosExtraidosJson;
                      }
                    })()
                  : "—"}
              </pre>
            </details>
            <div className="ia-revisao-acoes">
              <button type="button" className="ghost small-btn" onClick={() => void abrirHistorico(item.id)}>
                Ver histórico
              </button>
              {item.status === "PROCESSADO" && item.pendenciaId ? (
                <button
                  type="button"
                  className="ghost small-btn"
                  onClick={async () => {
                    try {
                      const d = await apiFetchJson<unknown>(`/api/pendencias/${item.pendenciaId}/dados-extraidos`, {
                        sessao
                      });
                      setDadosPendencia(JSON.stringify(d, null, 2));
                    } catch (e) {
                      setDadosPendencia(e instanceof Error ? e.message : "Erro");
                    }
                  }}
                >
                  Ver dados extraídos
                </button>
              ) : null}
            </div>
          </li>
        ))}
      </ul>
      {!loading && itens.length === 0 ? <p className="muted-react">Nenhum documento na fila.</p> : null}

      {dadosPendencia ? (
        <div className="modal-backdrop-react" role="dialog" aria-modal="true">
          <div className="modal-react modal-react--wide">
            <h3>Dados extraídos (pendência)</h3>
            <pre className="modal-pre-react">{dadosPendencia}</pre>
            <button type="button" onClick={() => setDadosPendencia(null)}>
              Fechar
            </button>
          </div>
        </div>
      ) : null}

      {historico ? (
        <div className="modal-backdrop-react" role="dialog" aria-modal="true">
          <div className="modal-react modal-react--wide">
            <h3>Histórico #{histProcId}</h3>
            <div className="ia-hist-list">
              {historico.length === 0 ? (
                <p className="muted-react">Sem histórico.</p>
              ) : (
                historico.map((h) => (
                  <div key={`${h.criadoEm}-${h.acao}`} className="ia-hist-linha">
                    <strong>{h.acao}</strong>
                    <span className="muted-react small">
                      {h.criadoEm} — {h.usuarioNome}
                    </span>
                    {h.motivo ? <p>{h.motivo}</p> : null}
                  </div>
                ))
              )}
            </div>
            <button type="button" onClick={() => setHistorico(null)}>
              Fechar
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}
