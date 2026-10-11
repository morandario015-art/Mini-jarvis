# Jarvis Android — prototipo 0.1

Código fuente de una aplicación Android. Este ZIP NO es un APK instalable.

## Implementado en el código
- Entrada por botón de voz y texto, con respuesta hablada.
- «Jarvis, abre Gmail», «abre Truecaller», «abre XTB»: busca aplicaciones instaladas por su nombre. Si hay varias coincidencias, muestra una selección.
- «Jarvis, llama a Carol Carabajo»: consulta contactos con permiso y muestra una selección cuando corresponde. Abre el marcador con el número; debes pulsar llamar. No inicia llamadas automáticamente en este prototipo.
- «Recuerda que mañana tomo café» y «mi resumen»: recuerdos locales.
- «Crea recordatorio»: abre el calendario para que elijas actividad, fecha y aviso. Solo queda guardado si confirmas en tu calendario.

## Todavía pendiente
- Activación continua por «Jarvis» con pantalla cerrada.
- Llamada directa y selección específica de Truecaller como marcador.
- Conexión de Gmail para leer mensajes, resúmenes automáticos y notificaciones.
- Servicio de IA con credenciales guardadas en servidor, no en el APK.
- Lectura autorizada de pantalla XTB y datos externos para análisis. No realiza compras ni ventas.

## Compilar
Abrir esta carpeta en Android Studio, instalar Android SDK 35, usar JDK 17 y Gradle 8.9. Ejecutar `gradle assembleDebug` o Build > Build APK(s).
También se incluye `.github/workflows/android.yml`: al subir el proyecto a un repositorio GitHub, Actions compila y deja el APK en el artefacto Jarvis-Android-APK. No se ha subido a ningún repositorio.

## Estado de validación
Se verificaron la estructura XML y el contenido del paquete. El código Java aún no se ha compilado. No se ha compilado el APK ni probado en un dispositivo: este entorno no tiene Android SDK, Gradle ni compilador Java. El reconocimiento de voz depende del servicio Android instalado y puede requerir internet.

Permisos solicitados al usar cada función: micrófono y contactos. No se solicitan acceso de accesibilidad ni captura de pantalla en esta versión.
