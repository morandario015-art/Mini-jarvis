# Jarvis Android 0.5 — voz local y conversación con IA

Di «Jarvis»: saluda según la hora de Ecuador y recibe tus consultas. Vosk procesa la voz en el teléfono. Groq o Gemini generan respuestas con contexto de hasta cuatro intercambios, mantenidos solo en memoria. No se envía audio ni se guarda un chat local; cada proveedor procesa el texto bajo sus políticas.

Usa **Conectar IA** para introducir la URL del servidor y el token personal, elegir proveedor y autorizar el envío de texto. Las claves Groq/Gemini solo se configuran en Cloudflare. El token del teléfono está cifrado con Android Keystore. **No se han desplegado ni probado las conexiones con las cuentas reales.**

«Mi presupuesto» informa de la asignación diaria y mensual. La nube bloquea al agotarla y reserva cada intento en una transacción SQLite, sin reintentos ni cambios automáticos de plan. Los límites internos se reparten hasta fin de mes, en hora de Ecuador. La gratuidad requiere cuentas sin facturación de pago habilitada; el código no puede comprobar esa configuración ni asegurar precios futuros. Consulta [cloud/README.md](cloud/README.md).

«Nueva conversación» borra el contexto. «Guardar respuesta en Word» guarda la última redacción en Descargas/Jarvis. «Abreme wasap» y «puedes abrir el WhatsApp» admiten variantes comunes. Las llamadas requieren elegir y confirmar contacto en pantalla y se preparan con ACTION_DIAL. Las acciones de apps/llamadas/archivos con pantalla bloqueada requieren desbloquear Android.

Activa **Activar escucha de Jarvis** una vez, concede micrófono y notificaciones y espera «Escucha activa». No necesitas tocar un botón para cada consulta. Tras silencio o «terminar» vuelve a esperar la palabra Jarvis. No inicia automáticamente tras reinicio o cierre forzado. Consumo de batería, precisión acústica y Bluetooth requieren prueba física en el Infinix. No elude PIN, patrón ni biometría.

El modelo conversa y prepara texto. Con Accesibilidad y consentimiento adicional, AppPilot describe los controles de la app elegida y ejecuta hasta cinco pasos de pulsación, escritura o desplazamiento, verificando que la pantalla no cambió. Pide confirmación para controles sensibles y no concede permisos. No garantiza compatibilidad con todas las apps; pantallas sin controles accesibles requieren integración específica. No navega con fuentes verificadas. Gmail, Google Workspace y análisis XTB siguen pendientes. Los Excel y PowerPoint de prueba siguen siendo ejemplos básicos. Usa una voz española Android sin red para respuestas habladas.

Código/modelos Vosk Apache 2.0; licencias en assets/licenses. Las pruebas automatizadas verifican comandos, presupuestos y respuestas simuladas; no sustituyen pruebas físicas o conexión real.

Sin burbuja ni superposición: al activar escucha se cierra y retira la pantalla de configuración. El servicio y su notificación Android permanecen activos. La interrupción mientras habla es experimental y usa cancelación de eco cuando Android la ofrece; requiere prueba en altavoz/Bluetooth y repetir la orden tras interrumpir.
