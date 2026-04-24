/** GET público — sem token. */
export async function fetchFeatures(): Promise<{ certificadoDigital: boolean }> {
  const res = await fetch("/api/features");
  if (!res.ok) throw new Error("Falha ao carregar features.");
  return res.json() as Promise<{ certificadoDigital: boolean }>;
}
