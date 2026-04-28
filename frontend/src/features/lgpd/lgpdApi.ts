import { apiFetchJson } from "../../lib/api";
import { Sessao } from "../../lib/session";

export type ConsentimentoPendenteResponse = {
  pendente: boolean;
  versaoPolitica: string;
};

export type PoliticaPrivacidadeResponse = {
  versao: string;
  texto: string;
};

export async function checkConsentimentoPendente(sessao: Sessao): Promise<ConsentimentoPendenteResponse> {
  return apiFetchJson("/api/lgpd/consentimento/pendente", { sessao });
}

export async function registrarConsentimento(sessao: Sessao): Promise<void> {
  return apiFetchJson("/api/lgpd/consentimento", { method: "POST", sessao });
}

export async function getPoliticaPrivacidade(sessao: Sessao): Promise<PoliticaPrivacidadeResponse> {
  return apiFetchJson("/api/lgpd/politica-privacidade", { sessao });
}

export async function solicitarEsquecimento(sessao: Sessao, usuarioId: number): Promise<void> {
  return apiFetchJson(`/api/lgpd/esquecimento/${usuarioId}`, { method: "DELETE", sessao });
}
