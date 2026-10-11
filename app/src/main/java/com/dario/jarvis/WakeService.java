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
 private TextToSpeech voice;
 private boolean voiceReady=false,paused=false,destroyed=false;
 private Handler handler;
 private PowerManager.WakeLock cpu;
 private WindowManager windows;
 private TextView bubble;
 private static final String CHANNEL="jarvis-listening";
 public static void pauseListening(){if(active!=null)active.pause();}
 public static void resumeListening(){if(active!=null)active.resume();}
 public void onCreate(){super.onCreate();active=this;handler=new Handler(getMainLooper());getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Escucha de Jarvis",NotificationManager.IMPORTANCE_LOW));voice=new TextToSpeech(this,status->{voiceReady=status==TextToSpeech.SUCCESS&&LocalVoice.select(voice);if(voiceReady){voice.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){handler.post(()->{if(id.equals("wake"))listenCommands();else if(id.equals("locked"))resume();else resume();});}public void onError(String id){handler.post(()->resume());}});}});}
 public int onStartCommand(Intent intent,int flags,int startId){if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}try{Notification n=notification("Iniciando detector…");if(Build.VERSION.SDK_INT>=29)startForeground(70,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(70,n);if(detector!=null){resume();return START_NOT_STICKY;}detector=new OfflineWakeDetector(this,()->{if(!destroyed&&!paused){try{detector.start();update("Escucha activa. Di Jarvis.");}catch(Exception e){fail("No pude iniciar el micrófono.");}}},()->wake(),e->fail("No pude cargar el detector de voz. Activa la escucha de nuevo."));detector.load();cpu=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"jarvis:listener");cpu.acquire();showBubble();update("Preparando voz local. Espera unos segundos…");getSharedPreferences("wake",0).edit().putString("status","Escucha activa. Di Jarvis.").apply();}catch(Exception e){fail("No se pudo activar el detector ("+e.getClass().getSimpleName()+"). Revisa el permiso del micrófono.");}return START_NOT_STICKY;}
 private Notification notification(String text){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,WakeService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Jarvis · escucha por voz").setContentText(text).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Detener",stop).build()).build();}
 private void update(String text){if(!destroyed)getSystemService(NotificationManager.class).notify(70,notification(text));}
 private void pause(){handler.removeCallbacksAndMessages(null);if(detector!=null&&!paused){try{detector.stop();paused=true;update("Escuchando tu orden…");}catch(Exception e){fail("No pude pausar el detector.");}}}
 private void resume(){if(destroyed||detector==null||!paused)return;try{detector.start();paused=false;update("Escucha activa. Di Jarvis.");}catch(Exception e){fail("No pude reanudar la escucha. Actívala de nuevo.");}}
 private void wake(){if(paused||destroyed)return;pause();KeyguardManager lock=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);if(lock.isKeyguardLocked()){update("Te escuché. Desbloquea para dar una orden.");if(voiceReady)voice.speak("Aquí estoy. Desbloquea el teléfono para abrir aplicaciones.",TextToSpeech.QUEUE_FLUSH,null,"locked");else resume();return;}if(voiceReady)voice.speak("Sí, aquí estoy.",TextToSpeech.QUEUE_FLUSH,null,"wake");else listenCommands();}
 private void launchUnlock(){if(destroyed)return;try{startActivity(new Intent(this,UnlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));handler.postDelayed(()->resume(),20000);}catch(Exception e){update("Desbloquea el teléfono y llama a Jarvis de nuevo.");resume();}}
 private void listenCommands(){
  if(destroyed)return;
  if(commands==null)commands=new OfflineCommands(this);
  update("Escuchando orden sin abrir Jarvis…");
  commands.start(this::handleCommand, e->respond(e));
 }
 private void respond(String message){
  if(destroyed)return;
  update(message);
  if(voiceReady)voice.speak(message,TextToSpeech.QUEUE_FLUSH,null,"answer");
  else resume();
 }
 private void handleCommand(String raw){
  String text=Command.body(raw);
  if(text.startsWith("abrir "))text="abre "+text.substring(6);
  if(text.startsWith("abre ")){
   String query=Command.appName(text.substring(5));
   Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
   List<ResolveInfo> all=getPackageManager().queryIntentActivities(launcher,0);
   ResolveInfo found=null;int count=0;
   for(ResolveInfo item:all){
    String label=Command.clean(item.loadLabel(getPackageManager()).toString());
    if(label.equals(query)||label.contains(query)){found=item;count++;}
   }
   if(count==0){respond("No encontré esa aplicación.");return;}
   if(count>1){respond("Encontré varias aplicaciones parecidas. Abre Jarvis para elegir.");return;}
   Intent launch=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
   launch.setComponent(new ComponentName(found.activityInfo.packageName,found.activityInfo.name));
   launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try{startActivity(launch);respond("Abriendo "+query);}catch(Exception e){respond("Android impidió abrirla desde segundo plano. Abre Jarvis para continuar.");}
   return;
  }
  if(text.contains("hola")||text.contains("como estas")){respond("Estoy aquí y escuchándote.");return;}
  respond("Te escuché. Por ahora puedo abrir aplicaciones por su nombre. Para otras tareas abre Jarvis.");
 }
 private void showBubble(){if(!Settings.canDrawOverlays(this))return;windows=(WindowManager)getSystemService(WINDOW_SERVICE);bubble=new TextView(this);bubble.setText("J");bubble.setTextSize(18);bubble.setTextColor(0xff071725);bubble.setGravity(Gravity.CENTER);bubble.setBackgroundColor(0xff70d9e8);int size=(int)(42*getResources().getDisplayMetrics().density);WindowManager.LayoutParams params=new WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.END;params.y=(int)(120*getResources().getDisplayMetrics().density);try{windows.addView(bubble,params);}catch(Exception e){bubble=null;}}
 private void fail(String text){getSharedPreferences("wake",0).edit().putString("status",text).apply();update(text);stopForeground(STOP_FOREGROUND_REMOVE);getSystemService(NotificationManager.class).notify(71,new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("Jarvis no pudo activar la escucha").setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).build());stopSelf();}
 public IBinder onBind(Intent i){return null;}
 public void onDestroy(){destroyed=true;if(active==this)active=null;if(handler!=null)handler.removeCallbacksAndMessages(null);if(commands!=null)commands.cancel();if(detector!=null){try{detector.stop();}catch(Exception ignored){}detector.close();}if(commands!=null)commands.close();if(cpu!=null&&cpu.isHeld())cpu.release();if(bubble!=null&&windows!=null){try{windows.removeView(bubble);}catch(Exception ignored){}}if(voice!=null)voice.shutdown();super.onDestroy();}
}
