import { FormEvent, useEffect, useRef, useState } from "react";
import { apiFetchFormData, apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type PendenciaOpt = { id: number; templateDocumentoNome: string; status: string };

const LOTE_TIMEOUT_MS = 120_000;
const MAX_ARQUIVOS_LOTE = 25;

type LoteErro = { nomeArquivoOriginal: string; mensagem: string };
type LoteResponse = {
  entregas: { id: number; pendenciaId: number; nomeArquivoOriginal: string; enviadoEm: string; observacao: string | null }[];
  erros: LoteErro[];
  message: string | null;
};

function hojeCompetencia() {
  const d = new Date();
  return { ano: d.getFullYear(), mes: d.getMonth() + 1 };
}

export function ClienteUploadPage({ sessao }: { sessao: Sessao }) {
  const comp = hojeCompetencia();
  const [pendencias, setPendencias] = useState<PendenciaOpt[]>([]);
  const [pendenciaId, setPendenciaId] = useState("");
  const [arquivos, setArquivos] = useState<File[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [observacao, setObservacao] = useState("");
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState("");
  const [ok, setOk] = useState("");

  async function carregarPendencias() {
    try {
      const data = await apiFetchJson<
        { id: number; templateDocumentoNome: string; status: string }[]
      >(`/api/pendencias?ano=${comp.ano}&mes=${comp.mes}`, { sessao });
      setPendencias(data);
      if (data.length && !pendenciaId) {
        setPendenciaId(String(data[0].id));
      }
    } catch {
      setPendencias([]);
    }
  }

  useEffect(() => {
    void carregarPendencias();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function onSubmit(ev: FormEvent) {
    ev.preventDefault();
    setErro("");
    setOk("");
    if (!arquivos.length) {
      setErro("Selecione pelo menos um arquivo.");
      return;
    }
    if (arquivos.length > MAX_ARQUIVOS_LOTE) {
      setErro(`No máximo ${MAX_ARQUIVOS_LOTE} arquivos por envio.`);
      return;
    }
    if (!pendenciaId) {
      setErro("Selecione uma pendência.");
      return;
    }
    setLoading(true);
    try {
      if (arquivos.length === 1) {
        const fd = new FormData();
        fd.append("arquivo", arquivos[0]);
        if (observacao.trim()) fd.append("observacao", observacao.trim());
        await apiFetchFormData(`/api/pendencias/${pendenciaId}/entregas`, fd, sessao);
        setOk("Documento enviado e pendência marcada como ENVIADO.");
      } else {
        const fd = new FormData();
        arquivos.forEach((f) => fd.append("arquivos", f));
        if (observacao.trim()) fd.append("observacao", observacao.trim());
        const data = await apiFetchFormData<LoteResponse>(
          `/api/pendencias/${pendenciaId}/entregas/lote`,
          fd,
          sessao,
          LOTE_TIMEOUT_MS
        );
        if (data.erros.length === 0) {
          setOk(
            `${data.entregas.length} documento(s) enviado(s) e pendência marcada como ENVIADO.`
          );
        } else {
          const detalhes = data.erros
            .slice(0, 8)
            .map((e) => `${e.nomeArquivoOriginal}: ${e.mensagem}`)
            .join(" · ");
          const mais =
            data.erros.length > 8 ? ` (+${data.erros.length - 8} outro(s))` : "";
          setOk([data.message, detalhes ? `${detalhes}${mais}` : ""].filter(Boolean).join(" "));
        }
      }
      setArquivos([]);
      if (fileInputRef.current) fileInputRef.current.value = "";
      setObservacao("");
      await carregarPendencias();
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Erro no upload.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page">
      <h2>Enviar documento</h2>
      <form className="cliente-upload-form" onSubmit={onSubmit}>
        <label>
          Pendência
          <select value={pendenciaId} onChange={(e) => setPendenciaId(e.target.value)} required>
            <option value="">— Selecione —</option>
            {pendencias.map((p) => (
              <option key={p.id} value={p.id}>
                #{p.id} {p.templateDocumentoNome} ({p.status})
              </option>
            ))}
          </select>
        </label>
        <label>
          Arquivo(s)
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept=".pdf,.xml,.png,.jpg,.jpeg,application/pdf,text/xml,application/xml,image/png,image/jpeg"
            onChange={(e) => setArquivos(Array.from(e.target.files ?? []))}
            required
          />
        </label>
        <p className="muted-react small">
          PDF, XML, PNG ou JPG — até {MAX_ARQUIVOS_LOTE} arquivos por envio. Um arquivo usa o fluxo único; vários usam
          envio em lote (cada arquivo gravado de forma independente).
        </p>
        {arquivos.length > 0 ? (
          <p className="muted-react small">
            {arquivos.length} arquivo(s) selecionado(s): {arquivos.map((f) => f.name).join(", ")}
          </p>
        ) : null}
        <label>
          Observação
          <input
            type="text"
            value={observacao}
            onChange={(e) => setObservacao(e.target.value)}
            placeholder="Opcional (vale para todos no lote)"
          />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? "Enviando..." : arquivos.length > 1 ? "Enviar lote" : "Enviar documento"}
        </button>
        {erro ? <p className="erro">{erro}</p> : null}
        {ok ? <p className="ok">{ok}</p> : null}
      </form>
    </section>
  );
}
