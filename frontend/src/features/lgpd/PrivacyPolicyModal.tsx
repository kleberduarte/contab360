import { useEffect, useState } from "react";
import { Sessao } from "../../lib/session";
import { getPoliticaPrivacidade, registrarConsentimento } from "./lgpdApi";

type Props = {
  sessao: Sessao;
  onAceito: () => void;
};

export function PrivacyPolicyModal({ sessao, onAceito }: Props) {
  const [texto, setTexto] = useState<string | null>(null);
  const [versao, setVersao] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    getPoliticaPrivacidade(sessao)
      .then((r) => {
        setTexto(r.texto);
        setVersao(r.versao);
      })
      .catch(() => setErro("Não foi possível carregar a política de privacidade."));
  }, [sessao]);

  async function aceitar() {
    setEnviando(true);
    setErro("");
    try {
      await registrarConsentimento(sessao);
      onAceito();
    } catch {
      setErro("Erro ao registrar consentimento. Tente novamente.");
      setEnviando(false);
    }
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.modal} role="dialog" aria-modal="true" aria-labelledby="lgpd-title">
        <div style={styles.header}>
          <h2 id="lgpd-title" style={styles.title}>Política de Privacidade</h2>
          <span style={styles.versao}>{versao}</span>
        </div>
        <div style={styles.body}>
          {texto == null && !erro ? (
            <p style={{ color: "#64748b" }}>Carregando...</p>
          ) : erro ? (
            <p style={{ color: "#dc2626" }}>{erro}</p>
          ) : (
            <pre style={styles.pre}>{texto}</pre>
          )}
        </div>
        <div style={styles.footer}>
          <p style={styles.aviso}>
            Para continuar usando o sistema, você precisa aceitar nossa Política de Privacidade
            em conformidade com a LGPD (Lei nº 13.709/2018).
          </p>
          {erro && !texto && null}
          {erro && texto && <p style={{ color: "#dc2626", fontSize: "0.85rem" }}>{erro}</p>}
          <button
            type="button"
            onClick={aceitar}
            disabled={enviando || !texto}
            style={{
              ...styles.btn,
              opacity: enviando || !texto ? 0.6 : 1,
              cursor: enviando || !texto ? "not-allowed" : "pointer"
            }}
          >
            {enviando ? "Registrando..." : "Li e aceito a Política de Privacidade"}
          </button>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  overlay: {
    position: "fixed",
    inset: 0,
    background: "rgba(0,0,0,0.55)",
    zIndex: 9999,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    padding: "1rem"
  },
  modal: {
    background: "#fff",
    borderRadius: "12px",
    width: "100%",
    maxWidth: "640px",
    maxHeight: "90vh",
    display: "flex",
    flexDirection: "column",
    boxShadow: "0 8px 40px rgba(0,0,0,0.22)"
  },
  header: {
    padding: "1.25rem 1.5rem 1rem",
    borderBottom: "1px solid #e2e8f0",
    display: "flex",
    alignItems: "center",
    gap: "0.75rem"
  },
  title: {
    margin: 0,
    fontSize: "1.15rem",
    fontWeight: 600,
    color: "#0f172a"
  },
  versao: {
    fontSize: "0.75rem",
    background: "#f1f5f9",
    color: "#475569",
    borderRadius: "6px",
    padding: "2px 8px"
  },
  body: {
    flex: 1,
    overflowY: "auto",
    padding: "1.25rem 1.5rem"
  },
  pre: {
    whiteSpace: "pre-wrap",
    wordBreak: "break-word",
    fontSize: "0.83rem",
    lineHeight: 1.65,
    color: "#334155",
    margin: 0,
    fontFamily: "inherit"
  },
  footer: {
    padding: "1rem 1.5rem 1.25rem",
    borderTop: "1px solid #e2e8f0",
    display: "flex",
    flexDirection: "column",
    gap: "0.75rem"
  },
  aviso: {
    fontSize: "0.82rem",
    color: "#64748b",
    margin: 0
  },
  btn: {
    background: "#21c25e",
    color: "#fff",
    border: "none",
    borderRadius: "8px",
    padding: "0.7rem 1.25rem",
    fontWeight: 600,
    fontSize: "0.95rem",
    width: "100%"
  }
};
