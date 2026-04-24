import { FormEvent, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type Pendencia = {
  id: number;
  templateDocumentoNome: string;
  vencimento: string;
  status: string;
  observacaoAnalise: string | null;
};

function hojeCompetencia() {
  const d = new Date();
  return { ano: d.getFullYear(), mes: d.getMonth() + 1 };
}

function badgeCliente(status: string) {
  if (status === "VALIDADO") return { cls: "status-chip status-ok", label: "OK pela IA" };
  if (status === "REJEITADO") return { cls: "status-chip status-rejeitado", label: "Rejeitado pela IA" };
  if (status === "ENVIADO") return { cls: "status-chip status-analise", label: "Em análise da IA" };
  return { cls: "status-chip status-pendente", label: "Pendente de envio" };
}

export function ClientePendenciasPage({ sessao }: { sessao: Sessao }) {
  const init = hojeCompetencia();
  const [ano, setAno] = useState(init.ano);
  const [mes, setMes] = useState(init.mes);
  const [lista, setLista] = useState<Pendencia[]>([]);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");
  const [dadosModal, setDadosModal] = useState<string | null>(null);

  async function carregar() {
    setLoading(true);
    setErro("");
    try {
      const data = await apiFetchJson<Pendencia[]>(`/api/pendencias?ano=${ano}&mes=${mes}`, { sessao });
      setLista(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao listar pendências.");
      setLista([]);
    } finally {
      setLoading(false);
    }
  }

  async function verDadosExtraidos(pendenciaId: number) {
    setErro("");
    try {
      const data = await apiFetchJson<unknown>(`/api/pendencias/${pendenciaId}/dados-extraidos`, { sessao });
      setDadosModal(JSON.stringify(data, null, 2));
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar dados.");
    }
  }

  function onFiltro(ev: FormEvent) {
    ev.preventDefault();
    void carregar();
  }

  useEffect(() => {
    void carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <section className="page">
      <h2>Minhas pendências</h2>
      <form className="cliente-filtro-react" onSubmit={onFiltro}>
        <label>
          Ano
          <input type="number" min={2000} max={2100} value={ano} onChange={(e) => setAno(Number(e.target.value))} />
        </label>
        <label>
          Mês
          <input type="number" min={1} max={12} value={mes} onChange={(e) => setMes(Number(e.target.value))} />
        </label>
        <button type="submit">Atualizar lista</button>
      </form>
      {erro ? <p className="erro">{erro}</p> : null}
      {loading ? <p className="muted-react">Carregando...</p> : null}
      {!loading && lista.length === 0 ? <p className="muted-react">Nenhuma pendência encontrada.</p> : null}
      <ul className="cliente-lista-pendencias">
        {lista.map((p) => {
          const b = badgeCliente(p.status);
          return (
            <li key={p.id} className="cliente-pendencia-item">
              <div className="cliente-pendencia-head">
                <span>
                  #{p.id} {p.templateDocumentoNome} — vence {p.vencimento}
                </span>
                <span className={b.cls}>{b.label}</span>
              </div>
              {p.observacaoAnalise ? (
                <p className="muted-react small">Motivo: {p.observacaoAnalise}</p>
              ) : null}
              {p.status === "VALIDADO" ? (
                <button type="button" className="ghost small-btn" onClick={() => void verDadosExtraidos(p.id)}>
                  Ver dados extraídos
                </button>
              ) : null}
            </li>
          );
        })}
      </ul>

      {dadosModal ? (
        <div className="modal-backdrop-react" role="dialog" aria-modal="true">
          <div className="modal-react">
            <h3>Dados extraídos</h3>
            <pre className="modal-pre-react">{dadosModal}</pre>
            <button type="button" onClick={() => setDadosModal(null)}>
              Fechar
            </button>
          </div>
        </div>
      ) : null}
    </section>
  );
}
