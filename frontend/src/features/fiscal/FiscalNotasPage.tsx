import { FormEvent, useEffect, useState } from "react";
import { apiFetchJson } from "../../lib/api";
import { apiFetchBlob } from "../../lib/blob";
import { Sessao } from "../../lib/session";

type NotaFiscal = {
  id: number;
  documentoEmitente: string;
  documentoDestinatario: string;
  tipoDocumento: string;
  tipoOperacao: string;
  valorTotal: number;
  dataEmissao: string;
  municipio: string;
  uf: string;
  chaveAcesso?: string | null;
  protocoloAutorizacao?: string | null;
  sefazModo?: string | null;
};

type RadarNotasResponse = {
  totalNotas: number;
  porOperacao: Record<string, number>;
  porTipoDocumento: Record<string, number>;
};

function somenteDigitos(value: string): string {
  return value.replace(/\D/g, "");
}

export function FiscalNotasPage({ sessao }: { sessao: Sessao }) {
  const [notas, setNotas] = useState<NotaFiscal[]>([]);
  const [radar, setRadar] = useState<RadarNotasResponse | null>(null);
  const [erro, setErro] = useState("");
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);
  const [ultimaEmitida, setUltimaEmitida] = useState<NotaFiscal | null>(null);

  const [emitente, setEmitente] = useState("");
  const [destinatario, setDestinatario] = useState("");
  const [tipoDoc, setTipoDoc] = useState("NFSE");
  const [tipoOp, setTipoOp] = useState("SAIDA");
  const [valor, setValor] = useState("10");
  const [dataEmissao, setDataEmissao] = useState(() => new Date().toISOString().slice(0, 10));
  const [municipio, setMunicipio] = useState("São Paulo");
  const [uf, setUf] = useState("SP");
  const [nfseOpen, setNfseOpen] = useState(false);
  const [nfseNumeroExibicao, setNfseNumeroExibicao] = useState("");
  const [nfseCodigoVerificacao, setNfseCodigoVerificacao] = useState("");
  const [nfseRazaoEmitente, setNfseRazaoEmitente] = useState("");
  const [nfseRazaoTomador, setNfseRazaoTomador] = useState("");
  const [nfseEnderecoEmitente, setNfseEnderecoEmitente] = useState("");
  const [nfseEnderecoTomador, setNfseEnderecoTomador] = useState("");
  const [nfseImEmitente, setNfseImEmitente] = useState("");
  const [nfseImTomador, setNfseImTomador] = useState("");
  const [nfseDiscriminacao, setNfseDiscriminacao] = useState("");
  const [nfseEmailTomador, setNfseEmailTomador] = useState("");
  const [nfseCodigoServicoTexto, setNfseCodigoServicoTexto] = useState("");
  const [nfseValorDeducoes, setNfseValorDeducoes] = useState("");
  const [nfseAliquotaIss, setNfseAliquotaIss] = useState("");
  const [nfseCreditoIptu, setNfseCreditoIptu] = useState("");
  const [nfseDataVencimentoIss, setNfseDataVencimentoIss] = useState("");

  async function carregarListas() {
    try {
      const [n, r] = await Promise.all([
        apiFetchJson<NotaFiscal[]>("/api/fiscal/notas", { sessao }),
        apiFetchJson<RadarNotasResponse>("/api/fiscal/radar", { sessao })
      ]);
      setNotas(n);
      setRadar(r);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro ao carregar notas/radar.");
    }
  }

  useEffect(() => {
    void carregarListas();
  }, [sessao]);

  function preencherMassaPmsp() {
    setEmitente("99999999000100");
    setDestinatario("99999999000147");
    setTipoDoc("NFSE");
    setValor("10");
    setDataEmissao("2011-08-03");
    setMunicipio("São Paulo");
    setUf("SP");
    setNfseNumeroExibicao("00002701");
    setNfseCodigoVerificacao("HXBA-6GMC");
    const raz = "INSCRICAO PARA TESTE NFE - PJ/0001";
    setNfseRazaoEmitente(raz);
    setNfseRazaoTomador(raz);
    const end = "R PEDRO AMERICO 00032, 27 ANDAR - CENTRO - CEP: 01045-010";
    setNfseEnderecoEmitente(end);
    setNfseEnderecoTomador(end);
    setNfseImEmitente("3.961.999-4");
    setNfseImTomador("3.961.999-0");
    setNfseDiscriminacao("teste");
    setNfseEmailTomador("-----");
    setNfseCodigoServicoTexto("08567 - Centros de emagrecimento, spa e congêneres.");
    setNfseValorDeducoes("0");
    setNfseAliquotaIss("5");
    setNfseCreditoIptu("0");
    setNfseDataVencimentoIss("2011-09-10");
    setNfseOpen(true);
    setMsg("Massa de teste PMSP preenchida. Confira e clique em Emitir nota.");
  }

  async function emitir(ev: FormEvent) {
    ev.preventDefault();
    setErro("");
    setMsg("");
    setLoading(true);
    try {
      const base: Record<string, unknown> = {
        documentoEmitente: somenteDigitos(emitente),
        documentoDestinatario: somenteDigitos(destinatario),
        tipoDocumento: tipoDoc,
        tipoOperacao: tipoOp,
        valorTotal: Number(valor),
        dataEmissao: dataEmissao,
        municipio: municipio.trim(),
        uf: uf.trim().toUpperCase()
      };
      const put = (key: string, val: string | undefined, num = false) => {
        if (val == null || val.trim() === "") return;
        if (num) {
          const n = Number(val);
          if (!Number.isNaN(n)) base[key] = n;
        } else {
          base[key] = val;
        }
      };
      put("nfseNumeroExibicao", nfseNumeroExibicao);
      put("nfseCodigoVerificacao", nfseCodigoVerificacao);
      put("nfseRazaoEmitente", nfseRazaoEmitente);
      put("nfseRazaoTomador", nfseRazaoTomador);
      put("nfseEnderecoEmitente", nfseEnderecoEmitente);
      put("nfseEnderecoTomador", nfseEnderecoTomador);
      put("nfseInscricaoMunicipalEmitente", nfseImEmitente);
      put("nfseInscricaoMunicipalTomador", nfseImTomador);
      put("nfseDiscriminacao", nfseDiscriminacao);
      put("nfseEmailTomador", nfseEmailTomador);
      put("nfseCodigoServicoTexto", nfseCodigoServicoTexto);
      put("nfseValorDeducoes", nfseValorDeducoes, true);
      put("nfseAliquotaIss", nfseAliquotaIss, true);
      put("nfseCreditoIptu", nfseCreditoIptu, true);
      if (nfseDataVencimentoIss) base.nfseDataVencimentoIss = nfseDataVencimentoIss;

      const criada = await apiFetchJson<NotaFiscal>("/api/fiscal/notas", {
        method: "POST",
        body: JSON.stringify(base),
        sessao
      });
      setUltimaEmitida(criada);
      setDataEmissao(new Date().toISOString().slice(0, 10));
      setMsg("Nota registrada.");
      await carregarListas();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao emitir.");
    } finally {
      setLoading(false);
    }
  }

  async function baixarDanfe(id: number) {
    setErro("");
    setMsg("");
    try {
      const blob = await apiFetchBlob(`/api/fiscal/notas/${id}/danfe-simulacao.pdf`, sessao);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `danfe-simulacao-${id}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
      setMsg("Download do PDF simulado iniciado.");
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao baixar PDF.");
    }
  }

  return (
    <section className="page fiscal-page-react">
      <h2>Notas fiscais</h2>
      <p className="muted-react small">Emissão de teste e radar (modo conforme backend).</p>
      {erro ? <p className="erro">{erro}</p> : null}
      {msg ? <p className="ok">{msg}</p> : null}

      <div className="fiscal-toolbar-react">
        <button type="button" className="ghost small-btn" onClick={() => void carregarListas()}>
          Atualizar lista / radar
        </button>
        <button type="button" className="ghost small-btn" onClick={preencherMassaPmsp}>
          Preencher massa PMSP (teste)
        </button>
      </div>

      <form className="fiscal-form-react" onSubmit={emitir}>
        <h3>Nova nota</h3>
        <div className="fiscal-form-grid-react">
          <label>
            Emitente (CNPJ)
            <input value={emitente} onChange={(e) => setEmitente(e.target.value)} required />
          </label>
          <label>
            Destinatário (CNPJ)
            <input value={destinatario} onChange={(e) => setDestinatario(e.target.value)} required />
          </label>
          <label>
            Tipo documento
            <select value={tipoDoc} onChange={(e) => setTipoDoc(e.target.value)}>
              <option value="NFE">NFE</option>
              <option value="NFSE">NFSE</option>
              <option value="NFCE">NFCE</option>
              <option value="CTE">CTE</option>
              <option value="MDFE">MDFE</option>
              <option value="CTE_OS">CTE_OS</option>
            </select>
          </label>
          <label>
            Operação
            <select value={tipoOp} onChange={(e) => setTipoOp(e.target.value)}>
              <option value="ENTRADA">ENTRADA</option>
              <option value="SAIDA">SAIDA</option>
            </select>
          </label>
          <label>
            Valor
            <input type="number" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required />
          </label>
          <label>
            Data emissão
            <input type="date" value={dataEmissao} onChange={(e) => setDataEmissao(e.target.value)} required />
          </label>
          <label>
            Município
            <input value={municipio} onChange={(e) => setMunicipio(e.target.value)} required />
          </label>
          <label>
            UF
            <input value={uf} onChange={(e) => setUf(e.target.value)} maxLength={2} required />
          </label>
        </div>

        <details className="fiscal-nfse-details-react" open={nfseOpen} onToggle={(e) => setNfseOpen((e.target as HTMLDetailsElement).open)}>
          <summary>Campos opcionais NFS-e (SP / cartão teste)</summary>
          <div className="fiscal-form-grid-react">
            <label>
              Nº exibição
              <input value={nfseNumeroExibicao} onChange={(e) => setNfseNumeroExibicao(e.target.value)} />
            </label>
            <label>
              Cód. verificação
              <input value={nfseCodigoVerificacao} onChange={(e) => setNfseCodigoVerificacao(e.target.value)} />
            </label>
            <label className="fiscal-span-2-react">
              Razão emitente
              <input value={nfseRazaoEmitente} onChange={(e) => setNfseRazaoEmitente(e.target.value)} />
            </label>
            <label className="fiscal-span-2-react">
              Razão tomador
              <input value={nfseRazaoTomador} onChange={(e) => setNfseRazaoTomador(e.target.value)} />
            </label>
            <label className="fiscal-span-2-react">
              Endereço emitente
              <input value={nfseEnderecoEmitente} onChange={(e) => setNfseEnderecoEmitente(e.target.value)} />
            </label>
            <label className="fiscal-span-2-react">
              Endereço tomador
              <input value={nfseEnderecoTomador} onChange={(e) => setNfseEnderecoTomador(e.target.value)} />
            </label>
            <label>
              IM emitente
              <input value={nfseImEmitente} onChange={(e) => setNfseImEmitente(e.target.value)} />
            </label>
            <label>
              IM tomador
              <input value={nfseImTomador} onChange={(e) => setNfseImTomador(e.target.value)} />
            </label>
            <label className="fiscal-span-2-react">
              Discriminação
              <input value={nfseDiscriminacao} onChange={(e) => setNfseDiscriminacao(e.target.value)} />
            </label>
            <label>
              E-mail tomador
              <input value={nfseEmailTomador} onChange={(e) => setNfseEmailTomador(e.target.value)} />
            </label>
            <label className="fiscal-span-2-react">
              Código serviço
              <input value={nfseCodigoServicoTexto} onChange={(e) => setNfseCodigoServicoTexto(e.target.value)} />
            </label>
            <label>
              Valor deduções
              <input value={nfseValorDeducoes} onChange={(e) => setNfseValorDeducoes(e.target.value)} />
            </label>
            <label>
              Alíquota ISS
              <input value={nfseAliquotaIss} onChange={(e) => setNfseAliquotaIss(e.target.value)} />
            </label>
            <label>
              Crédito IPTU
              <input value={nfseCreditoIptu} onChange={(e) => setNfseCreditoIptu(e.target.value)} />
            </label>
            <label>
              Vencimento ISS
              <input type="date" value={nfseDataVencimentoIss} onChange={(e) => setNfseDataVencimentoIss(e.target.value)} />
            </label>
          </div>
        </details>

        <button type="submit" disabled={loading}>
          {loading ? "Emitindo..." : "Emitir nota"}
        </button>
      </form>

      {ultimaEmitida ? (
        <div className="fiscal-pos-emitir-react">
          <h4>Última emissão #{ultimaEmitida.id}</h4>
          <p className="muted-react small">
            Modo: {ultimaEmitida.sefazModo ?? "—"} · Chave: <code>{ultimaEmitida.chaveAcesso ?? "—"}</code> · Protocolo:{" "}
            <code>{ultimaEmitida.protocoloAutorizacao ?? "—"}</code>
          </p>
          <button type="button" className="ghost small-btn" onClick={() => void baixarDanfe(ultimaEmitida.id)}>
            Baixar PDF (DANFE simulação)
          </button>
          <p className="muted-react small">Documento apenas para teste — sem valor fiscal.</p>
        </div>
      ) : null}

      <div className="fiscal-radar-react">
        <h3>Radar</h3>
        <pre className="fiscal-pre-react">{radar ? JSON.stringify(radar, null, 2) : "—"}</pre>
      </div>

      <div className="fiscal-lista-react">
        <h3>Notas registradas</h3>
        <table className="empresas-react-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Tipo</th>
              <th>Valor</th>
              <th>Data</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {notas.map((n) => (
              <tr key={n.id}>
                <td>{n.id}</td>
                <td>{n.tipoDocumento}</td>
                <td>{n.valorTotal}</td>
                <td>{n.dataEmissao}</td>
                <td>
                  <button type="button" className="ghost small-btn" onClick={() => void baixarDanfe(n.id)}>
                    PDF
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
