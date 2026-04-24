import { FormEvent, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type CadastroDocumentoFiscal = {
  id: number;
  tipoDocumento: string;
  documento: string;
  nome: string;
  ativo: boolean;
};

function somenteDigitos(value: string): string {
  return value.replace(/\D/g, "");
}

export function FiscalCadastrosPage({ sessao }: { sessao: Sessao }) {
  const [lista, setLista] = useState<CadastroDocumentoFiscal[]>([]);
  const [tipoDocumento, setTipoDocumento] = useState("CNPJ");
  const [documento, setDocumento] = useState("");
  const [nome, setNome] = useState("");
  const [erro, setErro] = useState("");
  const [msg, setMsg] = useState("");

  async function carregar() {
    try {
      const data = await apiFetchJson<CadastroDocumentoFiscal[]>("/api/fiscal/cadastros", { sessao });
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
      await apiFetchJson("/api/fiscal/cadastros", {
        method: "POST",
        body: JSON.stringify({
          tipoDocumento,
          documento: somenteDigitos(documento),
          nome: nome.trim()
        }),
        sessao
      });
      setDocumento("");
      setNome("");
      setMsg("Cadastro fiscal salvo.");
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao salvar.");
    }
  }

  return (
    <section className="page fiscal-page-react">
      <h2>Cadastros CPF/CNPJ</h2>
      {erro ? <p className="erro">{erro}</p> : null}
      {msg ? <p className="ok">{msg}</p> : null}
      <form className="fiscal-form-react" onSubmit={salvar}>
        <div className="fiscal-form-grid-react">
          <label>
            Tipo
            <select value={tipoDocumento} onChange={(e) => setTipoDocumento(e.target.value)}>
              <option value="CNPJ">CNPJ</option>
              <option value="CPF">CPF</option>
            </select>
          </label>
          <label>
            Documento (só dígitos)
            <input value={documento} onChange={(e) => setDocumento(e.target.value)} required />
          </label>
          <label className="fiscal-span-2-react">
            Nome / razão
            <input value={nome} onChange={(e) => setNome(e.target.value)} required />
          </label>
        </div>
        <button type="submit">Salvar cadastro</button>
      </form>
      <h3>Lista</h3>
      <table className="empresas-react-table">
        <thead>
          <tr>
            <th>Tipo</th>
            <th>Documento</th>
            <th>Nome</th>
          </tr>
        </thead>
        <tbody>
          {lista.map((c) => (
            <tr key={c.id}>
              <td>{c.tipoDocumento}</td>
              <td>{c.documento}</td>
              <td>{c.nome}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
