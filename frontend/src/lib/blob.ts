import { Sessao } from "./session";

export async function apiFetchBlob(url: string, sessao: Sessao | null): Promise<Blob> {
  const headers = new Headers();
  if (sessao?.token) headers.set("Authorization", `Bearer ${sessao.token}`);
  const res = await fetch(url, { headers });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(t || `Erro HTTP ${res.status}`);
  }
  return res.blob();
}
