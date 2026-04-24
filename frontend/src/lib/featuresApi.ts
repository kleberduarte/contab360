const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";

/** GET público — sem token. */
export async function fetchFeatures(): Promise<{ certificadoDigital: boolean }> {
  const res = await fetch(API_BASE + "/api/features");
  if (!res.ok) throw new Error("Falha ao carregar features.");
  return res.json() as Promise<{ certificadoDigital: boolean }>;
}
