# Jarvis Android 0.4 — voz gratuita y local
Activación experimental con Vosk, sin Picovoice, cuentas, AccessKey ni suscripción.
Pulsa **Activar escucha de Jarvis**, concede micrófono/notificaciones/superposición y espera la notificación «Escucha activa». Di Jarvis, espera la respuesta y da una orden en español.
Los modelos inglés (activación) y español (órdenes) se incluyen en APK y se preparan localmente al primer uso. No se escriben audios ni transcripciones en archivos, preferencias o registros. Solo las notas pedidas explícitamente con «recuerda que» se guardan.
La conexión a internet permanece disponible para funciones futuras, pero la escucha y los comandos se procesan localmente. No se incluye envío de audio, telemetría ni cliente de IA. Gmail y análisis XTB siguen pendientes. La voz hablada solo usa voces españolas de Android que declaran funcionar sin red; si no hay una instalada, utiliza texto.
Pantalla bloqueada: responde y solicita desbloqueo de Android; PIN/patrón/biometría siguen requiriendo autenticación.
No arranca sola tras reinicio ni cierre forzado. Puede necesitar ajustes de batería del fabricante y consume batería mientras escucha. Micrófono ocupado, ruido y acento pueden afectar detección. No se ha probado en dispositivo físico.
La compilación verifica vocabulario Jarvis del modelo, silencio y comandos normalizados. No demuestra precisión acústica o duración de batería.
Código y modelos Vosk bajo Apache 2.0. Licencias incluidas en assets/licenses.

## Llamadas compuestas
Admite «abre Truecaller e intenta llamar a Carol Carabajo», «abre true cooler y llama a…» y llamadas simples. Selecciona contacto y número en Jarvis, luego abre ACTION_DIAL en Truecaller. No ejecuta ACTION_CALL. Si Truecaller no maneja ese intent, pregunta antes de usar el marcador habitual. Nombres parecidos como Carol/Karol solo se presentan como candidatos para confirmar; no se elige automáticamente. No se ha verificado la integración con la versión de Truecaller instalada en el teléfono del usuario.
