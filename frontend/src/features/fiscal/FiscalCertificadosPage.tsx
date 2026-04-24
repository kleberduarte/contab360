import { FormEvent, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { fetchFeatures } from "../../lib/featuresApi";
import { Sessao } from "../../lib/session";

const STATUS_OPTS = [
  "EM_ANALISE",
  "AGUARDANDO_BIOMETRIA",
  "EMITIDO",
  "INSTALADO",
  "APROVADO",
  "ENTREGUE",
  "CANCELADO",
  "RENOVACAO_PENDENTE"
] as const;

type CertRow = {
  id: number;
  documentoSolicitante: string;
  titular: string;
  emailContato: string;
  tipoCertificado: string;
  validadeMeses: number;
  status: string;
  dataVencimentoPrevista?: string | null;
  criadoEm?: string;
  empresa?: { id: number; razaoSocial?: string } | null;
};

type EmpresaOpt = { id: number; cnpj: string; razaoSocial: string };

function somenteDigitos(value: string): string {
  return value.replace(/\D/g, "");
}

export function FiscalCertificadosPage({ sessao }: { sessao: Sessao }) {
  const [featureOn, setFeatureOn] = useState<boolean | null>(null);
  const [lista, setLista] = useState<CertRow[]>([]);
  const [empresas, setEmpresas] = useState<EmpresaOpt[]>([]);
  const [documento, setDocumento] = useState("");
  const [titular, setTitular] = useState("");
  const [email, setEmail] = useState("");
  const [tipo, setTipo] = useState("A1");
  const [validade, setValidade] = useState(12);
  const [empresaId, setEmpresaId] = useState<string>("");
  const [vencPrev, setVencPrev] = useState("");
  const [obs, setObs] = useState("");
  const [erro, setErro] = useState("");
  const [msg, setMsg] = useState("");

  useEffect(() => {
    void (async () => {
      try {
        const f = await fetchFeatures();
        setFeatureOn(f.certificadoDigital === true);
      } catch {
        setFeatureOn(false);
      }
    })();
  }, []);

  async function carregar() {
    if (!featureOn) return;
    try {
      const data = await apiFetchJson<CertRow[]>("/api/fiscal/certificados", { sessao });
      setLista(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao listar.");
    }
  }

  useEffect(() => {
    void (async () => {
      try {
        const e = await apiFetchJson<EmpresaOpt[]>("/api/empresas", { sessao });
        setEmpresas(e);
      } catch {
        setEmpresas([]);
      }
    })();
  }, [sessao]);

  useEffect(() => {
    if (featureOn) void carregar();
  }, [featureOn, sessao]);

  async function salvar(ev: FormEvent) {
    ev.preventDefault();
    if (!featureOn) return;
    setErro("");
    setMsg("");
    try {
      const payload: Record<string, unknown> = {
        documentoSolicitante: somenteDigitos(documento),
        titular: titular.trim(),
        emailContato: email.trim(),
        tipoCertificado: tipo.trim(),
        validadeMeses: validade
      };
      const eid = empresaId ? Number(empresaId) : null;
      if (eid && Number.isFinite(eid)) payload.empresaId = eid;
      if (vencPrev) payload.dataVencimentoPrevista = vencPrev;
      if (obs.trim()) payload.observacaoInterna = obs.trim();

      await apiFetchJson("/api/fiscal/certificados", {
        method: "POST",
        body: JSON.stringify(payload),
        sessao
      });
      setDocumento("");
      setTitular("");
      setEmail("");
      setObs("");
      setMsg("Pedido de certificado registrado.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao registrar.");
    }
  }

  async function mudarStatus(id: number, status: string) {
    setErro("");
    try {
      await apiFetchJson(`/api/fiscal/certificados/${id}`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
        sessao
      });
      setMsg("Status atualizado.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao atualizar status.");
    }
  }

  if (featureOn === null) {
    return (
      <section className="page fiscal-page-react">
        <p className="muted-react">Carregando...</p>
      </section>
    );
  }

  if (!featureOn) {
    return (
      <section className="page fiscal-page-react">
        <h2>Certificados digitais</h2>
        <p className="muted-react">Módulo desativado neste ambiente (feature flag).</p>
      </section>
    );
  }

  return (
    <section className="page fiscal-page-react">
      <h2>Certificados digitais</h2>
      {erro ? <p className="erro">{erro}</p> : null}
      {msg ? <p className="ok">{msg}</p> : null}

      <form className="fiscal-form-react" onSubmit={salvar}>
        <div className="fiscal-form-grid-react">
          <label>
            CPF/CNPJ solicitante
            <input value={documento} onChange={(e) => setDocumento(e.target.value)} required />
          </label>
          <label>
            Titular
            <input value={titular} onChange={(e) => setTitular(e.target.value)} required />
          </label>
          <label>
            E-mail
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Tipo
            <input value={tipo} onChange={(e) => setTipo(e.target.value)} required />
          </label>
          <label>
            Validade (meses)
            <input
              type="number"
              min={1}
              value={validade}
              onChange={(e) => setValidade(Number(e.target.value))}
              required
            />
          </label>
          <label>
            Empresa (opcional)
            <select value={empresaId} onChange={(e) => setEmpresaId(e.target.value)}>
              <option value="">—</option>
              {empresas.map((e) => (
                <option key={e.id} value={e.id}>
                  {e.razaoSocial}
                </option>
              ))}
            </select>
          </label>
          <label>
            Vencimento previsto
            <input type="date" value={vencPrev} onChange={(e) => setVencPrev(e.target.value)} />
          </label>
          <label className="fiscal-span-2-react">
            Observação interna
            <input value={obs} onChange={(e) => setObs(e.target.value)} />
          </label>
        </div>
        <button type="submit">Registrar pedido</button>
      </form>

      <h3>Pedidos</h3>
      {lista.length === 0 ? (
        <p className="muted-react">Nenhum pedido ainda.</p>
      ) : (
        <table className="empresas-react-table">
          <thead>
            <tr>
              <th>Criado</th>
              <th>Titular</th>
              <th>Empresa</th>
              <th>Tipo</th>
              <th>Venc.</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {lista.map((row) => (
              <tr key={row.id}>
                <td>{row.criadoEm ? String(row.criadoEm).slice(0, 10) : "—"}</td>
                <td>{row.titular}</td>
                <td>{row.empresa?.razaoSocial ?? "—"}</td>
                <td>{row.tipoCertificado}</td>
                <td>{row.dataVencimentoPrevista ?? "—"}</td>
                <td>
                  <select
                    value={row.status}
                    onChange={(e) => void mudarStatus(row.id, e.target.value)}
                    aria-label="Status do pedido"
                  >
                    {STATUS_OPTS.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
