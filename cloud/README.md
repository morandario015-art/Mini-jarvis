# Jarvis 0.5: conversación con Groq y Gemini

Android conserva la activación «Jarvis» y el reconocimiento Vosk en el teléfono. Tras activarlo, responde con un saludo y recibe órdenes; al terminar o pasar un intervalo sin voz vuelve a esperar «Jarvis». La conversación envía **texto** al servidor, no audio. El contexto se mantiene en memoria hasta cuatro intercambios; «nueva conversación» lo borra. No se guarda historial de chat en el servidor ni en el almacenamiento Android. Cada proveedor tiene su propia política: el contenido de Gemini gratuito puede utilizarse para mejorar productos.

## Qué implementa esta versión

- Conversación escrita y hablada, redacción y explicación con el proveedor elegido.
- Cliente HTTPS autenticado, sin redirecciones ni reintentos automáticos.
- Token personal cifrado mediante Android Keystore. Las claves de IA viven únicamente como secretos de Cloudflare.
- Abrir aplicaciones con variantes como «abreme wasap» y «puedes abrir el WhatsApp». Desambiguar varias coincidencias requiere pantalla; el modelo no ejecuta herramientas ni decide contactos.
- Guardar la última respuesta como Word en Descargas/Jarvis, por petición explícita.
- «Mi presupuesto» y «cuántas consultas quedan» consultan los contadores sin llamar a Groq/Gemini.
- Bloqueo de reservas simultáneas mediante SQLite y transacciones de Durable Objects.

## Gratuidad y presupuesto

Los máximos internos siguen siendo Groq 20/día y 400/mes; Gemini 15/día y 300/mes. Ahora el límite diario efectivo reparte el remanente entre los días que faltan, incluyendo hoy. En un mes nuevo de 31 días comienza con 12 consultas/día de Groq y 9 de Gemini. Si un día se usa menos, el reparto posterior se recalcula. No se puede adelantar todo el presupuesto mensual. Los días y meses se calculan con hora de Ecuador, UTC-5. Advertencia al 80 %, bloqueo al agotar la asignación. Los intentos fallidos también cuentan. No hay cambio automático de proveedor o plan.

**Estos límites no verifican facturación ni son cuotas oficiales.** Para evitar cargos, mantener los proyectos de Gemini y Groq y la cuenta Cloudflare en planes gratuitos, sin facturación de pago habilitada. Las cuotas reales, tokens y modelos gratuitos pueden cambiar y ser inferiores; un error detiene el intento. Este código no contrata servicios ni habilita pagos. El indicador FREE_TIER_CONFIRMED es una confirmación manual; no consulta la cuenta de facturación. No usar como garantía de coste en cuentas con facturación habilitada. Tampoco controla consumo de otras aplicaciones que utilicen las mismas claves. Internet móvil puede tener costes según tu operador.

## Activación en las cuentas existentes

La creación de una cuenta no crea automáticamente una clave ni conecta el aplicativo. El código preparado aquí no despliega cambios ni configura secretos en tus cuentas.

1. Verificar en las consolas que los planes son gratuitos y no tienen facturación de pago habilitada. Obtener las claves en GroqCloud y Google AI Studio sin activar pago.
2. Desde `cloud/`, iniciar sesión en Cloudflare con `npx wrangler login`.
3. Configurar únicamente en Cloudflare los secretos `GROQ_API_KEY` y `GEMINI_API_KEY` con `npx wrangler secret put NOMBRE`. No compartirlos en chat, GitHub ni APK.
4. Crear un token aleatorio de al menos 32 caracteres y configurarlo como secreto `JARVIS_ACCESS_TOKEN`. Este es el token que se introduce en el teléfono; no es ninguna clave de los proveedores.
5. Después de verificar el paso 1, cambiar `FREE_TIER_CONFIRMED` a `"true"` en `wrangler.toml`. Confirmar que los modelos seleccionados siguen disponibles en el nivel gratuito de esos proyectos. No cambia automáticamente de modelo.
6. Desplegar con `npx wrangler deploy`. Wrangler crea el Durable Object SQLite indicado en la migración; no necesita KV. No borrar ni recrear ese objeto para recuperar cuota. Si se migra una instalación que ya consumió consultas, importar sus contadores antes de habilitar uso; esta versión parte de un gateway todavía no desplegado.
7. Abrir `GET /health`: devuelve versión 0.5 y configuración general, sin secretos. En el APK: **Conectar IA**, URL HTTPS del Worker, token personal, Groq o Gemini, consentimiento, **Guardar y comprobar**. La prueba consulta `/budget`; no consume una consulta de IA.
8. Activar escucha y conceder los permisos Android. Probar «Jarvis», esperar saludo, pedir una explicación y luego «hazlo más breve». Probar «mi presupuesto», «abre WhatsApp» y «terminar».

Si el Worker sigue respondiendo Hello World, el aplicativo lo indica. Hasta configurar el servidor y las claves, las funciones locales permanecen disponibles.

## Pruebas y límites pendientes

`node --test cloud/worker.test.js` verifica reparto diario/mensual, cambio de fecha Ecuador, autenticación, bloqueo sin configuración, historial, errores de proveedor y concurrencia con SQLite real. GitHub Actions compila el APK y comprueba alias. Son pruebas con respuestas de IA simuladas: no demuestran conexión real a tus cuentas.

Falta la prueba física de reconocimiento/acento, batería, bloqueo y Bluetooth en el Infinix HOT 60 Pro+. Android puede exigir iniciar la escucha desde la app, conceder superposición y ajustar batería; no inicia automáticamente tras reinicio/cierre forzado. Usa una voz Android española que funcione sin red. No elude PIN, patrón ni biometría. El modelo ayuda a interpretar consultas, pero el reconocimiento de audio puede cometer errores.

La nube solo produce texto: Gmail, Google Docs/Sheets/Slides OAuth, navegación con fuentes y herramientas IA no están implementados. Los Excel/PowerPoint existentes siguen siendo ejemplos básicos, no documentos generados desde el contenido de una conversación.

Fuentes de implementación consultadas: https://developers.cloudflare.com/durable-objects/api/sqlite-storage-api/ ; https://developers.cloudflare.com/durable-objects/platform/pricing/ ; https://ai.google.dev/api/generate-content ; https://ai.google.dev/gemini-api/docs/pricing ; https://console.groq.com/docs/api-reference .
