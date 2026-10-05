/**
 * Backend de NutriAI: único lugar donde vive la API key del proveedor de IA.
 *
 * POST /v1/analyze  { image_base64, mime_type, locale }
 *   -> 200 { items: [{ food_name, estimated_quantity, unit, confidence }], overall_confidence }
 *   -> 422 si la imagen no muestra comida reconocible
 *   -> 4xx/5xx en otros errores (la app muestra un mensaje humano)
 *
 * La foto se procesa en memoria y no se almacena. La IA solo identifica alimentos y estima
 * cantidades: las calorías las calcula la app con su base nutricional (USDA).
 */
import Anthropic from "@anthropic-ai/sdk";
import { z } from "zod";
import { zodOutputFormat } from "@anthropic-ai/sdk/helpers/zod";

export interface Env {
  ANTHROPIC_API_KEY: string;
}

const MODEL = "claude-opus-5-5";
const MAX_IMAGE_BYTES = 4 * 1024 * 1024;
const ALLOWED_MIME = new Set(["image/jpeg", "image/png", "image/webp"]);

const RequestSchema = z.object({
  image_base64: z.string().min(100),
  mime_type: z.string(),
  locale: z.string().default("es"),
});

const UNITS = ["g", "ml", "unidad", "taza", "cucharada", "cucharadita", "porción", "rebanada", "pieza"] as const;

const AnalysisSchema = z.object({
  is_food: z.boolean(),
  items: z.array(
    z.object({
      food_name: z.string(),
      estimated_quantity: z.number(),
      unit: z.enum(UNITS),
      confidence: z.number(),
    }),
  ),
  overall_confidence: z.number(),
});

const SYSTEM_PROMPT = `Eres el módulo de reconocimiento de alimentos de una app de nutrición.
Recibes una foto de una comida. Tu trabajo es solo identificar los alimentos visibles y estimar su cantidad.
No calcules calorías: la app las obtiene de una base de datos nutricional.

- Nombra cada alimento en español, de forma genérica y buscable (p. ej. "arroz blanco", "frijoles negros", "pechuga de pollo", "plátano maduro frito"). Si reconoces un plato típico (p. ej. "gallo pinto", "casado"), usa su nombre.
- Estima la cantidad con la unidad más natural: gramos para sólidos servidos, ml para bebidas, o unidad/rebanada/pieza/taza cuando sea más claro.
- confidence (0 a 1) refleja lo seguro que estás de la identificación Y la cantidad. Sé honesto: si la foto es ambigua, usa valores bajos. No inventes alimentos que no se vean.
- overall_confidence (0 a 1) resume la fiabilidad del análisis completo.
- Si la imagen no muestra comida, responde is_food=false e items vacío.`;

// Límite básico por IP (memoria del isolate; orientativo). Para producción, usar el
// Rate Limiting de Cloudflare o un token de app verificado.
const hits = new Map<string, { count: number; reset: number }>();
function rateLimited(ip: string): boolean {
  const now = Date.now();
  const entry = hits.get(ip);
  if (!entry || entry.reset < now) {
    hits.set(ip, { count: 1, reset: now + 60_000 });
    return false;
  }
  entry.count += 1;
  return entry.count > 10;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method !== "POST" || url.pathname !== "/v1/analyze") return json({ error: "not_found" }, 404);
    if (rateLimited(request.headers.get("cf-connecting-ip") ?? "unknown")) return json({ error: "rate_limited" }, 429);

    const parsedRequest = RequestSchema.safeParse(await request.json().catch(() => null));
    if (!parsedRequest.success) return json({ error: "bad_request" }, 400);
    const { image_base64, mime_type } = parsedRequest.data;
    if (!ALLOWED_MIME.has(mime_type)) return json({ error: "unsupported_image" }, 422);
    if ((image_base64.length * 3) / 4 > MAX_IMAGE_BYTES) return json({ error: "image_too_large" }, 422);

    const client = new Anthropic({ apiKey: env.ANTHROPIC_API_KEY });
    try {
      const response = await client.beta.messages.create({
        model: MODEL,
        max_tokens: 4000,
        // Si un clasificador de seguridad rechaza la petición, la API la reintenta en el
        // modelo de respaldo recomendado.
        betas: ["server-side-fallback-2026-07-01"],
        fallbacks: "default",
        output_config: { effort: "medium", format: zodOutputFormat(AnalysisSchema) },
        system: SYSTEM_PROMPT,
        messages: [
          {
            role: "user",
            content: [
              {
                type: "image",
                source: { type: "base64", media_type: mime_type as "image/jpeg" | "image/png" | "image/webp", data: image_base64 },
              },
              { type: "text", text: "Identifica los alimentos de esta foto y estima sus cantidades." },
            ],
          },
        ],
      });

      if (response.stop_reason === "refusal" || response.stop_reason === "max_tokens") {
        return json({ error: "analysis_failed" }, 502);
      }
      const text = response.content.find((b) => b.type === "text");
      if (!text || text.type !== "text") return json({ error: "analysis_failed" }, 502);

      const analysis = AnalysisSchema.safeParse(JSON.parse(text.text));
      if (!analysis.success) return json({ error: "invalid_analysis" }, 502);
      if (!analysis.data.is_food || analysis.data.items.length === 0) return json({ error: "no_food_detected" }, 422);

      // La app vuelve a validar todo; aquí solo se recortan valores fuera de rango.
      const clamp = (n: number) => Math.min(1, Math.max(0, n));
      return json({
        items: analysis.data.items.map((i) => ({ ...i, confidence: clamp(i.confidence) })),
        overall_confidence: clamp(analysis.data.overall_confidence),
      });
    } catch (error) {
      if (error instanceof Anthropic.RateLimitError) return json({ error: "busy" }, 503);
      if (error instanceof Anthropic.BadRequestError) return json({ error: "unreadable_image" }, 422);
      if (error instanceof Anthropic.APIError) return json({ error: "provider_error" }, 502);
      return json({ error: "internal" }, 500);
    }
  },
};
