import { FormEvent, useEffect, useState } from "react";
import { apiFetchFormData, apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

type PendenciaOpt = { id: number; templateDocumentoNome: string; status: string };

function hojeCompetencia() {
  const d = new Date();
  return { ano: d.getFullYear(), mes: d.getMonth() + 1 };
}

export function ClienteUploadPage({ sessao }: { sessao: Sessao }) {
  const comp = hojeCompetencia();
  const [pendencias, setPendencias] = useState<PendenciaOpt[]>([]);
  const [pendenciaId, setPendenciaId] = useState("");
  const [arquivo, setArquivo] = useState<File | null>(null);
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
    if (!arquivo) {
      setErro("Selecione um arquivo.");
      return;
    }
    if (!pendenciaId) {
      setErro("Selecione uma pendência.");
      return;
    }
    setLoading(true);
    try {
      const fd = new FormData();
      fd.append("arquivo", arquivo);
      if (observacao.trim()) fd.append("observacao", observacao.trim());
      await apiFetchFormData(`/api/pendencias/${pendenciaId}/entregas`, fd, sessao);
      setOk("Documento enviado e pendência marcada como ENVIADO.");
      setArquivo(null);
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
          Arquivo
          <input
            type="file"
            onChange={(e) => setArquivo(e.target.files?.[0] ?? null)}
            required
          />
        </label>
        <label>
          Observação
          <input
            type="text"
            value={observacao}
            onChange={(e) => setObservacao(e.target.value)}
            placeholder="Opcional"
          />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? "Enviando..." : "Enviar documento"}
        </button>
        {erro ? <p className="erro">{erro}</p> : null}
        {ok ? <p className="ok">{ok}</p> : null}
      </form>
    </section>
  );
}
