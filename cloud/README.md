# JARVIS — arquitectura híbrida de IA (primera fase)

## Objetivo
Android local (voz, inventario, archivos) -> HTTPS Worker autenticado -> Groq o Gemini.
El Worker **no está desplegado**, ni la aplicación Android conectada todavía.

## Preparación
1. Crea cuentas gratuitas de Cloudflare Workers, GroqCloud y Google AI Studio. Confirma en cada consola que sigues en el plan gratuito y sin facturación de pago habilitada.
2. Instala Wrangler, inicia sesión con `npx wrangler login` y crea KV con `npx wrangler kv namespace create USAGE`. Pon su ID en `wrangler.toml`.
3. Configura secretos con `npx wrangler secret put GROQ_API_KEY`, `npx wrangler secret put GEMINI_API_KEY` y `npx wrangler secret put JARVIS_ACCESS_TOKEN` (token largo aleatorio).
4. Despliega con `npx wrangler deploy`. Nunca subas claves a GitHub ni las incrustes en un APK público.
5. Prueba `GET /budget` con cabecera `Authorization: Bearer TU_TOKEN`; y `POST /chat` con `{"provider":"groq","message":"Hola"}`.

## Presupuesto conservador inicial (configurable en worker.js)
Groq: **20 solicitudes/día y 400/mes**. Gemini: **15/día y 300/mes**.
Son límites **internos de JARVIS**, no cuotas oficiales de proveedores; los límites reales varían por modelo y cuenta. Se corta al alcanzar cualquiera de los dos. El endpoint /budget devuelve uso, advertencia al 80% y sugerencia diaria proporcional al resto del mes. No hay cambio automático a un plan de pago.

**Importante:** KV no tiene incrementos atómicos; solicitudes concurrentes podrían exceder el contador. Para un bloqueo estricto de cuota en producción usar Durable Objects o D1 con transacciones y límites también en el panel del proveedor. Los contadores contabilizan intentos, incluidos errores, para evitar reintentos sin límite. Las cuotas de tokens/minuto y búsquedas web todavía no se controlan; se usa salida limitada a 750 tokens. El proveedor puede rechazar antes por sus propias cuotas. La zona horaria de presupuestos es UTC.

## Tareas diarias sugeridas
Con el presupuesto inicial reserva Groq para ~10-13 conversaciones complejas al día y Gemini para ~8-10 documentos o explicaciones; el resto debe ejecutarse localmente. Estas cantidades son **propuestas**, no consumo garantizado. Usa GET /budget antes de asignar trabajo. Prioriza: (1) estudio urgente, (2) documentos, (3) investigación, (4) conversación recreativa.

## Seguridad y privacidad
El token personal es una primera capa, no una solución multiusuario. Para distribución pública implementar login por usuario, cuotas por cuenta y autenticación fuerte. No registrar mensajes sensibles. El contenido enviado a Groq/Gemini se procesa bajo sus políticas; revisar privacidad antes de enviar contactos o archivos personales.

## Pendiente
Conectar el cliente Android con la URL HTTPS del Worker y autenticación segura, persistencia de conversación, motor de herramientas con confirmaciones, búsqueda real con citas, Google Docs/Sheets/Slides OAuth, recordatorios locales, interrupciones de voz, pruebas de APK y pruebas de cuotas. **Este commit no convierte todavía la app en un asistente IA funcional.**
