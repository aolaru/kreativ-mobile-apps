import { defineConfig } from 'vite';
import { readFileSync } from 'node:fs';
export default defineConfig({
  plugins: [{ name: 'bundled-privacy-policy', resolveId(id) { if (id === './privacy-policy.txt?raw') return '\0kreativ-privacy-policy'; },
    load(id) { if (id === '\0kreativ-privacy-policy') return `export default ${JSON.stringify(readFileSync(new URL('./public/PRIVACY.txt', import.meta.url), 'utf8'))}`; } }],
  build: { target: 'chrome111' },
});
