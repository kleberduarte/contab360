import { FormEvent, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type Cobranca = {
  id: number;
  documentoPagador: string;
  valor: number;
  meioPagamento: string;
  vencimento: string;
  descricao: string;
  status: string;
};

function somenteDigitos(value: string): string {
  return value.replace(/\D/g, "");
}

export function FiscalCobrancasPage({ sessao }: { sessao: Sessao }) {
  const [lista, setLista] = useState<Cobranca[]>([]);
  const [documento, setDocumento] = useState("");
  const [valor, setValor] = useState("100");
  const [meio, setMeio] = useState("BOLETO");
  const [vencimento, setVencimento] = useState(() => new Date().toISOString().slice(0, 10));
  const [descricao, setDescricao] = useState("");
  const [erro, setErro] = useState("");
  const [msg, setMsg] = useState("");

  async function carregar() {
    try {
      const data = await apiFetchJson<Cobranca[]>("/api/fiscal/cobrancas", { sessao });
      setLista(data);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao listar.");
    }
  }

  useEffect(() => {
    void carregar();
  }, [sessao]);

  async function salvar(ev: FormEvent) {
    ev.preventDefault();
    setErro("");
    setMsg("");
    try {
      await apiFetchJson("/api/fiscal/cobrancas", {
        method: "POST",
        body: JSON.stringify({
          documentoPagador: somenteDigitos(documento),
          valor: Number(valor),
          meioPagamento: meio,
          vencimento,
          descricao: descricao.trim()
        }),
        sessao
      });
      setDocumento("");
      setDescricao("");
      setMsg("Cobrança gerada.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao gerar cobrança.");
    }
  }

  return (
    <section className="page fiscal-page-react">
      <h2>Cobranças</h2>
      {erro ? <p className="erro">{erro}</p> : null}
      {msg ? <p className="ok">{msg}</p> : null}
      <form className="fiscal-form-react" onSubmit={salvar}>
        <div className="fiscal-form-grid-react">
          <label>
            Documento pagador
            <input value={documento} onChange={(e) => setDocumento(e.target.value)} required />
          </label>
          <label>
            Valor
            <input type="number" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
          </label>
          <label>
            Meio
            <select value={meio} onChange={(e) => setMeio(e.target.value)}>
              <option value="BOLETO">BOLETO</option>
              <option value="PIX">PIX</option>
              <option value="CARTAO">CARTAO</option>
              <option value="DEBITO_AUTOMATICO">DEBITO_AUTOMATICO</option>
            </select>
          </label>
          <label>
            Vencimento
            <input type="date" value={vencimento} onChange={(e) => setVencimento(e.target.value)} required />
          </label>
          <label className="fiscal-span-2-react">
            Descrição
            <input value={descricao} onChange={(e) => setDescricao(e.target.value)} required />
          </label>
        </div>
        <button type="submit">Gerar cobrança</button>
      </form>
      <h3>Lista</h3>
      <table className="empresas-react-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Doc.</th>
            <th>Valor</th>
            <th>Meio</th>
            <th>Venc.</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {lista.map((c) => (
            <tr key={c.id}>
              <td>{c.id}</td>
              <td>{c.documentoPagador}</td>
              <td>{c.valor}</td>
              <td>{c.meioPagamento}</td>
              <td>{c.vencimento}</td>
              <td>{c.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
