# Selección para 0.3
Porcupine se retiró tras el usuario observar una prueba temporal. sherpa-onnx sigue siendo candidato de detector dedicado; para esta APK se eligió Vosk por su integración Java oficial comprobada y modelos móviles con licencia Apache 2.0.
Activación Jarvis: modelo inglés pequeño 0.15 y gramática Jarvis + desconocido. Comandos: modelo español pequeño 0.42 sin servicios cloud. Pausar/cerrar AudioRecord antes de comandos y respuesta; no mantener dos capturas simultáneas.
Modelos descargados en GitHub Actions e incluidos en assets; dispositivo no requiere descargar nada ni AccessKey.
No guardar PCM, hipótesis o conversaciones. Transcripciones viven en RAM/pantalla; preferencias solo contienen notas solicitadas y estado de servicio. Internet habilitado para extensiones futuras, sin tráfico de audio implementado.
Fuentes oficiales: https://alphacephei.com/vosk/android ; https://alphacephei.com/vosk/models ; https://github.com/alphacep/vosk-android-demo ; https://github.com/alphacep/vosk-api
