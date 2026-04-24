/**
 * Copia `frontend/dist` para `src/main/resources/static/react` (SPA Vite).
 * Uso: node scripts/copy-to-spring-static.mjs (após npm run build)
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, "..");
const dist = path.join(root, "dist");
const target = path.join(root, "..", "src", "main", "resources", "static", "react");

if (!fs.existsSync(dist)) {
  console.error("Pasta dist/ não encontrada. Rode npm run build no diretório frontend.");
  process.exit(1);
}

fs.rmSync(target, { recursive: true, force: true });
fs.mkdirSync(path.dirname(target), { recursive: true });
fs.cpSync(dist, target, { recursive: true });
console.log(`Copiado ${dist} -> ${target}`);
