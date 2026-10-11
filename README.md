# Jarvis Android — prueba 0.2
Aplicación experimental. GitHub Actions compila la APK, pero compilar no demuestra funcionamiento en tu dispositivo.

## Activación por voz
Esta versión integra Porcupine 4.0.2 y su palabra incorporada JARVIS. Requiere una AccessKey personal de Picovoice, que debes introducir en el teléfono en «Configurar activación por voz». Se guarda cifrada con Android Keystore. No la envíes por chat, ni la subas a GitHub. La disponibilidad, límites y licencia dependen de tu cuenta Picovoice.

1. Configura la clave en el teléfono.
2. Pulsa «Activar escucha de Jarvis», concede micrófono, notificaciones y permiso para mostrarse sobre otras apps. Regresa y vuelve a activar si acabas de otorgar el permiso.
3. Confirma que la notificación diga «Escucha activa».
4. Sal al inicio. Di «Jarvis», espera «Sí, aquí estoy», y después di «abre Truecaller».
5. Detén la escucha desde la app o la notificación. El indicador J aparece mientras el servicio está activo.

El detector se pausa antes de usar el reconocimiento de comandos para que no compitan por el micrófono. Solo los comandos usan el servicio de voz de Android; pueden requerir internet. El detector de la palabra usa audio local, pero Picovoice puede validar la clave por internet. La escucha usa CPU y batería.

## Teléfono bloqueado
El flujo implementado intenta responder, encender la pantalla y solicitar el desbloqueo OFICIAL de Android. Un bloqueo seguro requiere PIN, patrón o biometría del usuario; decir Jarvis no sustituye la autenticación. Si no hay bloqueo seguro o el sistema considera el teléfono de confianza, Android decide si puede descartarlo sin pedir credenciales. No se capturan ni almacenan credenciales de desbloqueo.

## Escenarios pendientes de prueba física
- App visible, pantalla principal, otra app abierta.
- Pantalla apagada y bloqueo con PIN/huella.
- Volumen bajo, ruido, acento, distancia y distintas pronunciaciones de Jarvis.
- Llamada o grabadora usando micrófono: el sistema puede silenciar al detector; detener y reactivar cuando termine.
- Batería restringida por Infinix/XOS: puede detener el servicio. Ajustar desde los controles del teléfono solo si se observa el problema.
- Reinicio, cierre forzado o revocación de permisos: hay que activar otra vez. No se inicia el micrófono automáticamente desde el arranque.
- Clave inválida, límites de dispositivo o fallo de red: se muestra error y se detiene la escucha.
- Apertura en segundo plano rechazada por Android: usar la notificación.
- La palabra JARVIS incorporada es un modelo del proveedor; no se ha validado con tu voz.

## Funciones anteriores
«abre Truecaller» acepta true caller, true cooler y tru caller. «llama a Carol Carabajo» busca contactos y permite elegir número para abrir el marcador. Todavía debes pulsar llamar. Los recuerdos son notas locales. El botón de recordatorio abre calendario; debes guardar el evento allí.
Gmail, IA, lectura XTB y ejecución de inversiones no están conectados.

## Compilar
JDK 17, Gradle 8.9, Android SDK 35; ejecutar gradle assembleDebug. La workflow ejecuta también las pruebas del normalizador de comandos. APK de prueba firmada con clave debug: no es una publicación en Play Store ni una versión de producción.
