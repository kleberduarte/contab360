import { FormEvent, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";
import { getSubscriptionStatus, subscribeToPush, unsubscribeFromPush } from "../../lib/pushNotification";

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

function competenciaAnterior(ano: number, mes: number) {
  if (mes > 1) {
    return { ano, mes: mes - 1 };
  }
  return { ano: ano - 1, mes: 12 };
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
  const [infoCompetencia, setInfoCompetencia] = useState("");
  const [dadosModal, setDadosModal] = useState<string | null>(null);
  const [pushStatus, setPushStatus] = useState<"granted" | "denied" | "default" | "unsupported">("default");
  const [pushLoading, setPushLoading] = useState(false);
  const [pushTestMsg, setPushTestMsg] = useState("");

  async function carregar() {
    setLoading(true);
    setErro("");
    setInfoCompetencia("");
    try {
      let anoBusca = ano;
      let mesBusca = mes;
      let data: Pendencia[] = [];

      for (let tentativa = 0; tentativa < 6; tentativa++) {
        data = await apiFetchJson<Pendencia[]>(`/api/pendencias?ano=${anoBusca}&mes=${mesBusca}`, { sessao });
        if (data.length > 0) {
          break;
        }
        const anterior = competenciaAnterior(anoBusca, mesBusca);
        anoBusca = anterior.ano;
        mesBusca = anterior.mes;
      }

      setLista(data);
      if (data.length > 0 && (anoBusca !== ano || mesBusca !== mes)) {
        setInfoCompetencia(`Mostrando pendências de ${String(mesBusca).padStart(2, "0")}/${anoBusca}.`);
      }
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
    void getSubscriptionStatus().then(setPushStatus);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handlePushToggle() {
    setPushLoading(true);
    setPushTestMsg("");
    try {
      if (pushStatus === "granted") {
        await unsubscribeFromPush(sessao.token);
        setPushStatus("default");
      } else {
        const ok = await subscribeToPush(sessao.token);
        setPushStatus(ok ? "granted" : await getSubscriptionStatus());
      }
    } finally {
      setPushLoading(false);
    }
  }

  async function handlePushTestSend() {
    setPushLoading(true);
    setErro("");
    setPushTestMsg("");
    try {
      const r = await apiFetchJson<{ ok: boolean; sent: number }>("/api/push/test-notify", {
        method: "POST",
        sessao,
      });
      setPushTestMsg(
        r.sent > 0
          ? `Teste enviado (${r.sent} dispositivo(s)). Confira a bandeja de notificações do Windows e minimize o navegador se não aparecer.`
          : "Nenhum envio com sucesso — veja o log do servidor."
      );
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha no teste de push.");
    } finally {
      setPushLoading(false);
    }
  }

  return (
    <section className="page">
      <div className="page-header-row">
        <h2>Minhas pendências</h2>
        {pushStatus !== "unsupported" && pushStatus !== "denied" && (
          <div className="push-header-actions">
            <button
              type="button"
              className={`ghost small-btn push-toggle-btn${pushStatus === "granted" ? " push-toggle-btn--active" : ""}`}
              onClick={() => void handlePushToggle()}
              disabled={pushLoading}
              title={pushStatus === "granted" ? "Desativar notificações" : "Ativar notificações de novas pendências"}
            >
              {pushStatus === "granted" ? "🔔 Notificações ativas" : "🔕 Ativar notificações"}
            </button>
            {pushStatus === "granted" ? (
              <button
                type="button"
                className="ghost small-btn"
                disabled={pushLoading}
                title="Envia uma notificação de teste pelo servidor"
                onClick={() => void handlePushTestSend()}
              >
                Testar envio
              </button>
            ) : null}
          </div>
        )}
        {pushStatus === "denied" && (
          <div className="push-denied-help">
            <p className="push-denied-help__compact">
              <strong>Bloqueado pelo navegador.</strong> Abra o ícone ao lado do endereço →{" "}
              <strong>Notificações → Permitir</strong>, depois:
            </p>
            <button
              type="button"
              className="ghost small-btn push-denied-help__recheck"
              disabled={pushLoading}
              onClick={() => {
                setPushLoading(true);
                void getSubscriptionStatus()
                  .then(setPushStatus)
                  .finally(() => setPushLoading(false));
              }}
            >
              Verificar de novo
            </button>
            <details className="push-denied-help__details">
              <summary>Onde achar nas configurações</summary>
              <ul>
                <li>
                  <strong>Chrome / Edge:</strong> ⋮ → Privacidade e segurança → Configurações do site → Notificações.
                </li>
                <li>
                  <strong>Firefox:</strong> ícone na URL → Permissões → Notificações.
                </li>
                <li>
                  Em produção é preciso <strong>HTTPS</strong>; em desenvolvimento, <code>localhost</code> basta.
                </li>
              </ul>
            </details>
          </div>
        )}
      </div>
      {pushTestMsg ? (
        <p className="cliente-push-test-msg" role="status">
          {pushTestMsg}
        </p>
      ) : null}
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
      {infoCompetencia ? (
        <div className="cliente-competencia-info" role="status" aria-live="polite">
          <span className="cliente-competencia-info__badge">Competência exibida</span>
          <p className="cliente-competencia-info__text">{infoCompetencia}</p>
        </div>
      ) : null}
      {loading ? <p className="muted-react">Carregando...</p> : null}
      {!loading && lista.length === 0 ? <p className="muted-react">Nenhuma pendência encontrada.</p> : null}
      <ul className="cliente-lista-pendencias">
        {lista.map((p) => {
          const b = badgeCliente(p.status);
          return (
            <li key={p.id} className="cliente-pendencia-item">
              <div className="cliente-pendencia-head">
                <div className="cliente-pendencia-head__main">
                  <p className="cliente-pendencia-head__title">{p.templateDocumentoNome}</p>
                  <p className="cliente-pendencia-head__meta">
                    <span className="cliente-pendencia-head__id">Ref {p.id}</span>
                    <span>Vencimento {p.vencimento}</span>
                  </p>
                </div>
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
