package com.dario.jarvis;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.speech.tts.*;
import android.view.*;
import android.widget.TextView;
import java.util.Locale;
import android.content.pm.ResolveInfo;
import java.util.List;

public class WakeService extends Service {
 private static WakeService active;
 private OfflineWakeDetector detector;
 private OfflineCommands commands;
 private boolean conversationActive=false;
 private final Runnable endConversation=()->{conversationActive=false;if(commands!=null)commands.cancel();resume();};
 private TextToSpeech voice;
 private boolean voiceReady=false,paused=false,destroyed=false;
 private Handler handler;
 private PowerManager.WakeLock cpu;
 private WindowManager windows;
 private AppPilot pilot;
 private VoiceTasks tasks;
 private VoiceInterruptor interruptor;
 private String spoken="";
 private static final String CHANNEL="jarvis-listening";
 public static void pauseListening(){if(active!=null)active.pause();}
 public static void resumeListening(){if(active!=null)active.resume();}
 public void onCreate(){super.onCreate();active=this;handler=new Handler(getMainLooper());tasks=new VoiceTasks(this);pilot=new AppPilot(this);interruptor=new VoiceInterruptor(this);AppInventory.refresh(this);getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Escucha de Jarvis",NotificationManager.IMPORTANCE_LOW));voice=new TextToSpeech(this,status->{voiceReady=status==TextToSpeech.SUCCESS&&LocalVoice.select(voice);if(voiceReady){voice.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){handler.post(()->{if(interruptor!=null)interruptor.start(spoken,()->{if(voice!=null)voice.stop();handler.postDelayed(WakeService.this::listenCommands,250);});});}public void onDone(String id){handler.post(()->{if(interruptor!=null)interruptor.stop();if(id.equals("wake"))listenCommands();else if(id.equals("locked"))resume();else afterResponse();});}public void onError(String id){handler.post(()->{if(interruptor!=null)interruptor.stop();afterResponse();});}});}});}
 public int onStartCommand(Intent intent,int flags,int startId){if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}try{Notification n=notification("Iniciando detector…");if(Build.VERSION.SDK_INT>=29)startForeground(70,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(70,n);if(detector!=null){resume();return START_NOT_STICKY;}detector=new OfflineWakeDetector(this,()->{if(!destroyed&&!paused){try{detector.start();update("Escucha activa. Di Jarvis.");}catch(Exception e){fail("No pude iniciar el micrófono.");}}},()->wake(),e->fail("No pude cargar el detector de voz. Activa la escucha de nuevo."));detector.load();cpu=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"jarvis:listener");cpu.acquire();update("Preparando voz local. Espera unos segundos…");getSharedPreferences("wake",0).edit().putString("status","Escucha activa. Di Jarvis.").apply();}catch(Exception e){fail("No se pudo activar el detector ("+e.getClass().getSimpleName()+"). Revisa el permiso del micrófono.");}return START_NOT_STICKY;}
 private Notification notification(String text){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,WakeService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Jarvis · escucha por voz").setContentText(text).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Detener",stop).build()).build();}
 private void update(String text){if(!destroyed)getSystemService(NotificationManager.class).notify(70,notification(text));}
 private void pause(){handler.removeCallbacksAndMessages(null);if(detector!=null&&!paused){try{detector.stop();paused=true;update("Escuchando tu orden…");}catch(Exception e){fail("No pude pausar el detector.");}}}
 private void resume(){if(destroyed||detector==null||!paused)return;conversationActive=false;handler.removeCallbacks(endConversation);try{detector.start();paused=false;update("Escucha activa. Di Jarvis.");}catch(Exception e){fail("No pude reanudar la escucha. Actívala de nuevo.");}}
 private void wake(){
  if(paused||destroyed)return;pause();
  int hour=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Guayaquil")).get(java.util.Calendar.HOUR_OF_DAY);
  String greeting=hour<12?"Buenos días":hour<19?"Buenas tardes":"Buenas noches";
  spoken="Hola, Darío. "+greeting+". Aquí estoy. ¿Cómo estás?";if(voiceReady)voice.speak(spoken,TextToSpeech.QUEUE_FLUSH,null,"wake");else listenCommands();
 }
 private void listenCommands(){
  if(destroyed)return;
  conversationActive=true;
  if(commands==null)commands=new OfflineCommands(this);
  update("Escuchando orden sin abrir Jarvis…");
  commands.start(this::handleCommand, e->endConversation.run());
 }
 private void afterResponse(){
  if(destroyed)return;
  if(!conversationActive){resume();return;}
  handler.removeCallbacks(endConversation);
  handler.postDelayed(endConversation,45000);
  handler.postDelayed(this::listenCommands,250);
 }
 private void respond(String message){
  if(destroyed)return;
  spoken=message;update("Jarvis está respondiendo.");
  if(voiceReady)voice.speak(message,TextToSpeech.QUEUE_FLUSH,null,"answer");
  else afterResponse();
 }
 private void handleCommand(String raw){
  String text=Command.body(raw);
  if(text.equals("resumen de aplicaciones")){if(!AppInventory.saved(this).isEmpty()){respond(AppInventory.summary(this));return;}}
  if(AppInventory.isInventoryCommand(text)){int count=AppInventory.refresh(this);respond("Reconocí "+count+" aplicaciones visibles para Jarvis. ¿Quieres el resumen detallado? Di Jarvis y luego sí.");return;}
  if(text.equals("terminar")||text.equals("adios")||text.equals("deja de escuchar")){conversationActive=false;tasks.cancel();pilot.cancel();respond("Conversación terminada.");return;}
  if(tasks.handle(raw,this::respond))return;
  Command.Call call=Command.call(raw);
  String query=Command.openAppQuery(raw);
  if(call!=null||query!=null){
   KeyguardManager lock=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
   if(lock.isKeyguardLocked()){conversationActive=false;respond("Desbloquea el teléfono para abrir aplicaciones o preparar llamadas.");return;}
  }
  if(query!=null){
   Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
   List<ResolveInfo> all=getPackageManager().queryIntentActivities(launcher,0);
   ResolveInfo found=null;int count=0;
   for(ResolveInfo item:all){
    String label=Command.clean(item.loadLabel(getPackageManager()).toString());
    if(Command.fuzzyApp(query,label)){found=item;count++;}
   }
   if(count==0){respond("No encontré esa aplicación.");return;}
   if(count>1){respond("Encontré varias aplicaciones parecidas. Repite el nombre completo para elegir una.");return;}
   Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
   launch.setComponent(new ComponentName(found.activityInfo.packageName,found.activityInfo.name));
   launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try{if(!AppActionsService.launch(launch))startActivity(launch);String packageName=found.activityInfo.packageName;String label=found.loadLabel(getPackageManager()).toString();pilot.opened(packageName,label,this::respond);}catch(Exception e){respond("Android impidió abrirla. Revisa el permiso de acciones por voz en la configuración inicial.");}
   return;
  }
  if(Command.isBudget(text)){queryCloud("",true);return;}
  if(text.equals("nueva conversacion")||text.equals("borra la conversacion")){CloudClient.get(this).clear();respond("Contexto borrado. ¿En qué trabajamos?");return;}
  if(text.equals("guardar respuesta en word")||text.equals("guarda la respuesta en word")){
   if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked()){conversationActive=false;respond("Desbloquea el teléfono para guardar el documento.");return;}
   String answer=CloudClient.get(this).lastAnswer();if(answer.isEmpty()){respond("Primero pide que redacte el contenido.");return;}
   try{DocumentEngine.save(this,"docx","Trabajo preparado con Jarvis",answer.split("\n"));respond("La respuesta está guardada como Word en Descargas, carpeta Jarvis.");}catch(Exception e){respond("No pude guardar el documento.");}return;
  }
  if(text.equals("hola")||text.equals("como estas")){respond("Estoy aquí, Darío. ¿En qué trabajamos?");return;}
  if(pilot.handle(raw,this::respond))return;
  queryCloud(raw,false);
 }
 private void queryCloud(String message,boolean budget){
  update(budget?"Consultando presupuesto…":"Pensando con "+CloudClient.get(this).provider()+"…");
  handler.removeCallbacks(endConversation);
  CloudClient.Callback callback=new CloudClient.Callback(){public void done(String answer){if(!destroyed)respond(answer);}public void failed(String error){if(!destroyed)respond(error);}};
  if(budget)CloudClient.get(this).budget(callback);else CloudClient.get(this).chat(message,callback);
 }
 private void fail(String text){getSharedPreferences("wake",0).edit().putString("status",text).apply();update(text);stopForeground(STOP_FOREGROUND_REMOVE);getSystemService(NotificationManager.class).notify(71,new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Jarvis no pudo activar la escucha").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).build());stopSelf();}
 public IBinder onBind(Intent i){return null;}
 public void onDestroy(){destroyed=true;if(active==this)active=null;if(handler!=null)handler.removeCallbacksAndMessages(null);if(commands!=null)commands.cancel();if(detector!=null){try{detector.stop();}catch(Exception ignored){}detector.close();}if(commands!=null)commands.close();if(cpu!=null&&cpu.isHeld())cpu.release();if(interruptor!=null)interruptor.close();if(tasks!=null)tasks.cancel();if(pilot!=null)pilot.cancel();if(voice!=null)voice.shutdown();super.onDestroy();}
}

