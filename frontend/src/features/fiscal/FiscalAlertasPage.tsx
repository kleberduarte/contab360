import { useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type AlertaFiscal = {
  id: number;
  tipo: string;
  documentoReferencia: string;
  mensagem: string;
  dataAlvo: string;
  resolvido: boolean;
  criadoEm: string;
};

type RelatorioEstrategicoResponse = {
  totalNotasEmitidas: number;
  totalCadastrosCpfCnpj: number;
  totalCobrancasGeradas: number;
  totalCertificadosVendidos: number;
  totalAlertasAbertos: number;
  totalPrefeiturasCompativeis: number;
};

export function FiscalAlertasPage({ sessao }: { sessao: Sessao }) {
  const [alertas, setAlertas] = useState<AlertaFiscal[]>([]);
  const [rel, setRel] = useState<RelatorioEstrategicoResponse | null>(null);
  const [pref, setPref] = useState<Record<string, unknown> | null>(null);
  const [erro, setErro] = useState("");
  const [msg, setMsg] = useState("");

  async function carregar() {
    setErro("");
    try {
      const [a, r] = await Promise.all([
        apiFetchJson<AlertaFiscal[]>("/api/fiscal/alertas", { sessao }),
        apiFetchJson<RelatorioEstrategicoResponse>("/api/fiscal/relatorios/estrategico", { sessao })
      ]);
      setAlertas(a);
      setRel(r);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar.");
    }
  }

  useEffect(() => {
    void carregar();
  }, [sessao]);

  async function gerarAutomaticos() {
    setMsg("");
    setErro("");
    try {
      const res = await apiFetchJson<{ alertasCriados: number }>("/api/fiscal/alertas/automaticos", {
        method: "POST",
        sessao
      });
      setMsg(`${res.alertasCriados ?? 0} alerta(s) criado(s).`);
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao gerar alertas.");
    }
  }

  async function carregarPrefeituras() {
    setErro("");
    try {
      const data = await apiFetchJson<Record<string, unknown>>("/api/fiscal/prefeituras/compatibilidade", { sessao });
      setPref(data);
      setMsg("Compatibilidade de prefeituras carregada.");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro.");
    }
  }

  return (
    <section className="page fiscal-page-react">
      <h2>Alertas e relatórios</h2>
      <p className="muted-react small">Alertas em aberto e painel estratégico consolidado.</p>
      {erro ? <p className="erro">{erro}</p> : null}
      {msg ? <p className="ok">{msg}</p> : null}

      <div className="fiscal-toolbar-react">
        <button type="button" className="ghost small-btn" onClick={() => void carregar()}>
          Atualizar
        </button>
        <button type="button" className="ghost small-btn" onClick={() => void gerarAutomaticos()}>
          Gerar alertas automáticos
        </button>
        <button type="button" className="ghost small-btn" onClick={() => void carregarPrefeituras()}>
          Compatibilidade prefeituras
        </button>
      </div>

      <div className="fiscal-rel-kpis-react">
        {rel ? (
          <ul className="ia-obs-lista-react">
            <li>
              Notas emitidas: <strong>{rel.totalNotasEmitidas}</strong>
            </li>
            <li>
              Cadastros CPF/CNPJ: <strong>{rel.totalCadastrosCpfCnpj}</strong>
            </li>
            <li>
              Cobranças: <strong>{rel.totalCobrancasGeradas}</strong>
            </li>
            <li>
              Certificados (pedidos): <strong>{rel.totalCertificadosVendidos}</strong>
            </li>
            <li>
              Alertas abertos: <strong>{rel.totalAlertasAbertos}</strong>
            </li>
            <li>
              Prefeituras compatíveis (referência): <strong>{rel.totalPrefeiturasCompativeis}</strong>
            </li>
          </ul>
        ) : (
          <p className="muted-react">—</p>
        )}
      </div>

      <pre className="fiscal-pre-react">{rel ? JSON.stringify(rel, null, 2) : "{}"}</pre>

      {pref ? (
        <div className="fiscal-pref-react">
          <h3>Prefeituras</h3>
          <pre className="fiscal-pre-react">{JSON.stringify(pref, null, 2)}</pre>
        </div>
      ) : null}

      <h3>Alertas em aberto</h3>
      {alertas.length === 0 ? (
        <p className="muted-react">Nenhum alerta em aberto.</p>
      ) : (
        <table className="empresas-react-table">
          <thead>
            <tr>
              <th>Tipo</th>
              <th>Documento</th>
              <th>Data alvo</th>
              <th>Mensagem</th>
            </tr>
          </thead>
          <tbody>
            {alertas.map((a) => (
              <tr key={a.id}>
                <td>{a.tipo}</td>
                <td>{a.documentoReferencia}</td>
                <td>{a.dataAlvo}</td>
                <td>{a.mensagem}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
