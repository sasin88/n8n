# NutriAI backend (Cloudflare Worker)

Puente seguro entre la app y el proveedor de IA. **La API key solo existe aquí**, como secreto
del Worker; la app únicamente conoce la URL de este backend.

- Recibe la foto (base64), la envía a Claude y devuelve solo alimentos + cantidades + confianza.
- No guarda las fotos.
- Cambiar de proveedor de IA = cambiar este archivo. La app no se toca.

## Puesta en marcha

1. Cuenta de Cloudflare (plan gratuito suficiente para empezar) y una API key de Anthropic.
2. `npm install`
3. `npx wrangler login`
4. `npx wrangler secret put ANTHROPIC_API_KEY`
5. `npx wrangler deploy` → anota la URL (`https://nutriai-backend.<tu-cuenta>.workers.dev`).
6. Compila la app con esa URL:
   - En GitHub: *Settings → Secrets and variables → Actions → Variables* → `NUTRIAI_AI_BACKEND_URL`.
   - O localmente: `./gradlew assembleRelease -Pnutriai.aiBackendUrl=https://...`

Sin URL configurada, la app usa el modo demostración (MOCK), indicado en pantalla.

## Costes

Cada análisis es una llamada a la API de Claude con una imagen (~1280 px). El coste por foto
depende del tamaño de la imagen y de la respuesta; consulta los precios vigentes en
https://www.anthropic.com/pricing. Cloudflare Workers tiene un plan gratuito con límite diario
de peticiones.

## Limitaciones conocidas

- El límite por IP es orientativo (memoria del isolate). Para producción conviene el Rate
  Limiting de Cloudflare y verificar que las peticiones vienen de la app (p. ej. Play Integrity).
