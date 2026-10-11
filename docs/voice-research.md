# Escenarios y soluciones de activación Android
Revisión: 11 octubre 2026.

| Ruta | Evidencia primaria | Aplicación a Jarvis |
|---|---|---|
| Porcupine | https://github.com/Picovoice/porcupine/tree/master/demo/android/Service ; https://picovoice.ai/docs/api/porcupine-android/ | Integramos JARVIS, servicio y traspaso a reconocimiento de comandos. Requiere clave del usuario. |
| Vosk | https://alphacephei.com/vosk/android ; https://github.com/alphacep/vosk-android-demo | Reconocimiento offline general; requiere modelo y validar vocabulario/acento. No lo elegimos como detector específico. |
| sherpa-onnx | https://k2-fsa.github.io/sherpa/onnx/kws/index.html | Detector offline con demo Android y modelos. Alternativa si Picovoice no conviene; requiere integrar runtime/modelos y calibrar Jarvis. |
| Asistente predeterminado Android | https://developer.android.com/reference/android/service/voice/VoiceInteractionService | Android mantiene el servicio elegido por el usuario. No basta declararlo: requiere selección del rol, sesión y detector compatible; no garantiza DSP para cualquier palabra. |
| Repetir SpeechRecognizer | https://developer.android.com/reference/android/speech/SpeechRecognizer | Android documenta que no está destinado al reconocimiento continuo; no lo usamos como detector permanente. |
| Desbloqueo oficial | https://developer.android.com/reference/android/app/KeyguardManager#requestDismissKeyguard(android.app.Activity,%20android.app.KeyguardManager.KeyguardDismissCallback) | Pide credenciales cuando el bloqueo es seguro. No admite reemplazar PIN por la palabra Jarvis. |
| Permisos y segundo plano | https://developer.android.com/develop/background-work/services/fgs/service-types ; https://developer.android.com/guide/components/activities/secure-bal | Servicio de tipo microphone iniciado con app visible; overlay autorizado para abrir interfaz tras llamada de voz. |

Los ejemplos prueban que estas rutas existen; no prueban funcionamiento en un Infinix X6886. La prueba de dispositivo debe cubrir los escenarios del README. No existe una lista exhaustiva de todas las implementaciones ni una garantía universal para todos los fabricantes.
