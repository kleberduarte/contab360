export type DocTabsTone = "suave" | "vibrante";

export const DOC_TABS_TONE_STORAGE_KEY = "contab360.docsValidados.docTabsTone";

export function getDocTabsTone(): DocTabsTone {
  const salvo = window.localStorage.getItem(DOC_TABS_TONE_STORAGE_KEY);
  return salvo === "vibrante" ? "vibrante" : "suave";
}

export function applyDocTabsTone(tone: DocTabsTone): void {
  if (tone === "vibrante") {
    document.body.setAttribute("data-doc-tabs-tone", "vibrante");
  } else {
    document.body.removeAttribute("data-doc-tabs-tone");
  }
}

export function persistDocTabsTone(tone: DocTabsTone): void {
  window.localStorage.setItem(DOC_TABS_TONE_STORAGE_KEY, tone);
}
