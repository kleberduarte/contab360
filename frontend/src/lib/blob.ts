import { Sessao } from "./session";

// Deve coincidir com api.ts: em PRD (Vercel) o backend fica em VITE_API_BASE_URL.
const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";

export async function apiFetchBlob(url: string, sessao: Sessao | null): Promise<Blob> {
  const headers = new Headers();
  if (sessao?.token) headers.set("Authorization", `Bearer ${sessao.token}`);
  const res = await fetch(API_BASE + url, { headers });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(t || `Erro HTTP ${res.status}`);
  }
  return res.blob();
}
