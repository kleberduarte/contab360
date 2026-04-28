import { Sessao } from "./session";

// Em produção (Vercel), aponta para o backend no Railway via VITE_API_BASE_URL.
// Em desenvolvimento, usa string vazia para aproveitar o proxy do Vite (/api -> localhost:8080).
const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";

let _onUnauthorized: (() => void) | null = null;

/** Registra o callback que será chamado quando qualquer request retornar 401. */
export function setOnUnauthorized(cb: () => void): void {
  _onUnauthorized = cb;
}

type RequestOptions = RequestInit & {
  sessao?: Sessao | null;
};

const REQUEST_TIMEOUT_MS = 20000;

async function fetchWithTimeout(input: RequestInfo | URL, init: RequestInit, timeoutMs = REQUEST_TIMEOUT_MS): Promise<Response> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(input, { ...init, signal: controller.signal });
  } finally {
    window.clearTimeout(timer);
  }
}

export async function apiFetchJson<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { sessao, headers, ...rest } = options;
  const method = (rest.method || "GET").toUpperCase();
  const isGet = method === "GET";
  const finalHeaders = new Headers(headers || {});

  if (!finalHeaders.has("Content-Type") && rest.body) {
    finalHeaders.set("Content-Type", "application/json");
  }
  if (sessao?.token) {
    finalHeaders.set("Authorization", `Bearer ${sessao.token}`);
  }
  if (isGet) {
    if (!finalHeaders.has("Cache-Control")) finalHeaders.set("Cache-Control", "no-cache");
    if (!finalHeaders.has("Pragma")) finalHeaders.set("Pragma", "no-cache");
  }

  const response = await fetchWithTimeout(API_BASE + url, {
    ...rest,
    headers: finalHeaders,
    cache: rest.cache ?? (isGet ? "no-store" : undefined)
  });

  if (!response.ok) {
    if (response.status === 401) {
      _onUnauthorized?.();
    }
    const text = await response.text();
    let msg = text || `Erro HTTP ${response.status}`;
    try {
      const err = JSON.parse(text) as { message?: string };
      if (err?.message) msg = err.message;
    } catch {
      // resposta não é JSON, mantém texto original
    }
    throw new Error(msg);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

/** POST multipart/form-data (não define Content-Type — o browser define boundary). */
export async function apiFetchFormData<T>(
  url: string,
  formData: FormData,
  sessao: Sessao | null,
  timeoutMs: number = REQUEST_TIMEOUT_MS
): Promise<T> {
  const headers = new Headers();
  if (sessao?.token) {
    headers.set("Authorization", `Bearer ${sessao.token}`);
  }
  const response = await fetchWithTimeout(
    API_BASE + url,
    {
      method: "POST",
      body: formData,
      headers,
      cache: "no-store"
    },
    timeoutMs
  );
  const text = await response.text();
  if (!response.ok) {
    if (response.status === 401) {
      _onUnauthorized?.();
    }
    let msg = text || `Erro HTTP ${response.status}`;
    try {
      const err = JSON.parse(text) as { message?: string };
      if (err.message) msg = err.message;
    } catch {
      /* texto não é JSON */
    }
    throw new Error(msg);
  }
  if (response.status === 204 || !text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}
