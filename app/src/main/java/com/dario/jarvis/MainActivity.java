package com.dario.jarvis;
import android.Manifest;
import android.app.*;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity implements RecognitionListener {
 private TextView output; private EditText input; private OfflineCommands recognizer; private TextToSpeech voice; private boolean voiceReady=false; private String pending=""; private String pendingApp=""; private boolean wakeRequested=false;
 public void onCreate(Bundle state){super.onCreate(state);
 LinearLayout root=new LinearLayout(this); root.setOrientation(1);root.setPadding(28,40,28,24);root.setBackgroundColor(0xff081725);
 ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 TextView title=new TextView(this);title.setText("JARVIS · ANDROID");title.setTextSize(28);title.setTextColor(0xff77deef);root.addView(title);
 TextView description=new TextView(this);description.setText("Conversa con IA · Abre aplicaciones · Prepara trabajos");description.setTextColor(0xffe1edf5);root.addView(description);
 Button active=new Button(this);active.setText("Activar escucha de Jarvis");root.addView(active);active.setOnClickListener(v->activateWake());
 Button stop=new Button(this);stop.setText("Detener escucha");root.addView(stop);stop.setOnClickListener(v->{stopService(new Intent(this,WakeService.class));say("Escucha detenida.");});
 TextView wakeInfo=new TextView(this);wakeInfo.setText("Activa la escucha. No necesitas cuenta ni clave. Después di Jarvis, espera la respuesta y da tu orden. En pantalla bloqueada debes desbloquear para abrir apps. Conecta Groq o Gemini para conversar. Gmail sigue pendiente.");wakeInfo.setTextColor(0xffb7d2df);root.addView(wakeInfo);
 Button connect=new Button(this);connect.setText("Conectar IA");root.addView(connect);connect.setOnClickListener(v->CloudSettings.show(this,this::say));
 Button budget=new Button(this);budget.setText("Mi presupuesto de consultas");root.addView(budget);budget.setOnClickListener(v->askCloud("",true));
 Button clear=new Button(this);clear.setText("Nueva conversación");root.addView(clear);clear.setOnClickListener(v->{CloudClient.get(this).clear();say("Contexto de conversación borrado.");});
 Button save=new Button(this);save.setText("Guardar última respuesta en Word");root.addView(save);save.setOnClickListener(v->saveAnswer());
 Button actions=new Button(this);actions.setText("Permitir acciones por voz en otras apps");root.addView(actions);actions.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Acciones en aplicaciones").setMessage("Android requiere activar Jarvis en Accesibilidad para usar controles de otras apps por voz. Puede leer etiquetas de controles y pulsar o escribir en la app que elijas. No lee contraseñas. Las acciones sensibles requieren confirmación.").setPositiveButton("Abrir ajustes",(d,n)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("Cancelar",null).show());
 Button contacts=new Button(this);contacts.setText("Permitir contactos para llamadas por voz");root.addView(contacts);contacts.setOnClickListener(v->requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},5));
 Button files=new Button(this);files.setText("Crear documentos de prueba (Word, Excel, PowerPoint)");root.addView(files);files.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Crear archivo local").setItems(new String[]{"Word (.docx)","Excel (.xlsx)","PowerPoint (.pptx)"},(d,n)->createSample(new String[]{"docx","xlsx","pptx"}[n])).show());
 Button talk=new Button(this);talk.setText("Hablar");root.addView(talk);talk.setOnClickListener(v->listen());
 input=new EditText(this);input.setHint("Jarvis, abre Truecaller");input.setTextColor(-1);input.setHintTextColor(0xffa0bacb);root.addView(input);
 Button send=new Button(this);send.setText("Enviar");root.addView(send);send.setOnClickListener(v->command(input.getText().toString()));
 Button summary=new Button(this);summary.setText("Mis recuerdos");root.addView(summary);summary.setOnClickListener(v->command("mi resumen"));
 Button calendar=new Button(this);calendar.setText("Crear recordatorio en calendario");root.addView(calendar);calendar.setOnClickListener(v->calendar());
 output=new TextView(this);output.setTextSize(18);output.setTextColor(-1);output.setPadding(0,24,0,24);root.addView(output);output.setText("Di Jarvis para comenzar. Usa Conectar IA para habilitar conversación con tu servidor. Gmail y análisis XTB siguen pendientes.");
 voice=new TextToSpeech(this,status->{voiceReady=status==TextToSpeech.SUCCESS&&LocalVoice.select(voice);if(voiceReady){voice.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){runOnUiThread(()->WakeService.resumeListening());}public void onError(String id){runOnUiThread(()->WakeService.resumeListening());}});}if(wakeRequested){wakeRequested=false;runOnUiThread(()->listen());}});
 recognizer=new OfflineCommands(this);
 if(getIntent().hasExtra("command")){input.setText(getIntent().getStringExtra("command"));command(getIntent().getStringExtra("command"));}else if(getIntent().getBooleanExtra("fromWake",false))wakeRequested=true;
 }
 private void say(String text){output.setText(text);if(voiceReady)voice.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis");else WakeService.resumeListening();}
 private void listen(){WakeService.pauseListening();if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;}if(voice!=null)voice.stop();output.setText("Preparando voz local… Habla después de unos segundos.");recognizer.start(text->{input.setText(text);command(text);},message->say(message));}
 private void command(String raw){
  String text=Command.body(raw);
  if(text.isEmpty()){say("Hola, Darío. Aquí estoy. ¿En qué trabajamos?");return;}
  if(Command.isBudget(text)){askCloud("",true);return;}
  if(text.equals("nueva conversacion")||text.equals("borra la conversacion")){CloudClient.get(this).clear();say("Contexto borrado. ¿En qué trabajamos ahora?");return;}
  if(text.equals("guardar respuesta en word")||text.equals("guarda la respuesta en word")){saveAnswer();return;}
  if(text.equals("dame todo el resumen")){if(!AppInventory.saved(this).isEmpty()){say(AppInventory.summary(this));return;}}
  if(AppInventory.isInventoryCommand(text)){int count=AppInventory.refresh(this);say("Reconocí "+count+" aplicaciones visibles para Jarvis. Para consultarlas, di resumen de aplicaciones.");return;}
  if(text.equals("resumen de aplicaciones")){say(AppInventory.summary(this));return;}
  Command.Call call=Command.call(raw);if(call!=null){findContact(call.name,call.app);return;}
  String app=Command.openAppQuery(raw);if(app!=null){openApp(app);return;}
  if(text.startsWith("recuerda que ")||text.startsWith("guarda que ")){String memory=text.substring(text.indexOf("que ")+4).trim();if(memory.isEmpty()){say("¿Qué quieres recordar?");return;}String previous=getPreferences(0).getString("mem","");getPreferences(0).edit().putString("mem",previous+memory+"\n").apply();say("Guardé: "+memory+". Esto es una nota; para recibir un aviso usa el calendario.");return;}
  if(text.equals("mi resumen")||text.equals("mis recuerdos")){String mem=getPreferences(0).getString("mem","");say(mem.isEmpty()?"Aún no tienes recuerdos guardados.":"Tus recuerdos:\n"+mem);return;}
  if(text.equals("crear recordatorio")||text.equals("abre el calendario")){calendar();return;}
  askCloud(raw,false);
 }
 private void askCloud(String message,boolean budget){
  WakeService.pauseListening();if(recognizer!=null)recognizer.cancel();if(voice!=null)voice.stop();
  output.setText(budget?"Consultando presupuesto…":"Pensando con "+CloudClient.get(this).provider()+"…");
  CloudClient.Callback callback=new CloudClient.Callback(){public void done(String answer){if(!isDestroyed())say(answer);else WakeService.resumeListening();}public void failed(String error){if(!isDestroyed())say(error);else WakeService.resumeListening();}};
  if(budget)CloudClient.get(this).budget(callback);else CloudClient.get(this).chat(message,callback);
 }
 private void saveAnswer(){
  String answer=CloudClient.get(this).lastAnswer();if(answer.isEmpty()){say("Primero pide a Jarvis que redacte el contenido.");return;}
  try{DocumentEngine.save(this,"docx","Trabajo preparado con Jarvis",answer.split("\n"));say("La última respuesta está guardada en Descargas, carpeta Jarvis, como documento Word.");}catch(Exception e){say("No pude guardar el documento.");}
 }
 private void createSample(String kind){try{
  String title=kind.equals("xlsx")?"Ejemplo de presupuesto":kind.equals("pptx")?"Presentación de ejemplo":"Documento de ejemplo";
  String[] rows=kind.equals("xlsx")?new String[]{"Concepto|Valor","Ingresos|100","Gastos|35","Ganancia|=B3-B4"}:new String[]{"Creado localmente por Jarvis.","Contenido de demostración. Indica tus datos para elaborar un trabajo real."};
  Uri uri=DocumentEngine.save(this,kind,title,rows);
  say("Archivo "+kind+" guardado en Descargas, carpeta Jarvis.");
  new AlertDialog.Builder(this).setTitle("Documento guardado").setMessage("¿Quieres abrirlo con una aplicación compatible?").setPositiveButton("Abrir",(d,n)->{try{DocumentEngine.open(this,uri,kind);}catch(Exception e){say("No encontré una aplicación compatible. El archivo está en Descargas.");}}).setNegativeButton("Después",null).show();
 }catch(Exception e){say("No pude crear el archivo: "+e.getMessage());}}
 private void openApp(String query){query=Command.canonicalApp(query);if(query.trim().isEmpty()){say("Indica el nombre de la aplicación.");return;}Intent launcher=new Intent(Intent.ACTION_MAIN);launcher.addCategory(Intent.CATEGORY_LAUNCHER);List<ResolveInfo> all=getPackageManager().queryIntentActivities(launcher,0);List<ResolveInfo> matches=new ArrayList<>();List<String> labels=new ArrayList<>();for(ResolveInfo r:all){String label=r.loadLabel(getPackageManager()).toString();String name=Command.clean(label);if(Command.fuzzyApp(query,name)){matches.add(r);labels.add(label);}}if(matches.isEmpty()){say("No encontré una aplicación llamada "+query+".");return;}if(matches.size()==1){launch(matches.get(0));return;}say("Hay varias aplicaciones. Elige una en pantalla.");new AlertDialog.Builder(this).setTitle("¿Qué aplicación?").setItems(labels.toArray(new String[0]),(d,n)->launch(matches.get(n))).setNegativeButton("Cancelar",null).show();}
 private void launch(ResolveInfo app){Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setComponent(new ComponentName(app.activityInfo.packageName,app.activityInfo.name));try{startActivity(i);}catch(Exception e){say("Android no permitió abrir esa aplicación.");}}
 private void findContact(String name){findContact(name,"");}
 private void findContact(String name,String app){if(name.trim().isEmpty()){say("Indica el nombre del contacto.");return;}if(!app.isEmpty()&&!app.equals("truecaller")){say("Puedo preparar llamadas en Truecaller o en el marcador del teléfono. Prueba: abre Truecaller y llama a "+name);return;}if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){pending=name;pendingApp=app;requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},2);return;}
 List<String> labels=new ArrayList<>(),numbers=new ArrayList<>(),nearLabels=new ArrayList<>(),nearNumbers=new ArrayList<>();Set<String> unique=new HashSet<>();String[] projection={ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};
 try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,projection,null,null,null)){if(c!=null)while(c.moveToNext()){String label=c.getString(0),number=c.getString(1);if(label==null||number==null||!unique.add(label+number))continue;if(Command.clean(label).contains(name)){labels.add(label+" · "+number);numbers.add(number);}else if(Command.nearName(label,name)){nearLabels.add(label+" · "+number);nearNumbers.add(number);}}}catch(Exception e){say("No pude consultar tus contactos.");return;}
 boolean approximate=numbers.isEmpty()&&!nearNumbers.isEmpty();if(approximate){labels.addAll(nearLabels);numbers.addAll(nearNumbers);}if(numbers.isEmpty()){say("No encontré ese contacto. Prueba con su nombre como está guardado.");return;}
 say(approximate?"Encontré nombres parecidos. Confirma el contacto en pantalla.":"Elige el número para abrir el marcador.");
 new AlertDialog.Builder(this).setTitle(approximate?"¿A cuál contacto te refieres?":"Llamar a "+name).setItems(labels.toArray(new String[0]),(d,n)->dial(numbers.get(n),app)).setNegativeButton("Cancelar",null).show();
 }
 private void dial(String number,String app){Intent dial=new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",number,null));if(app.equals("truecaller")){Intent preferred=new Intent(dial).setPackage("com.truecaller");try{startActivity(preferred);return;}catch(ActivityNotFoundException e){say("Truecaller no permite abrir este número. Puedes usar el marcador del teléfono.");new AlertDialog.Builder(this).setTitle("Abrir marcador del teléfono").setMessage("Truecaller no está disponible para esta acción. ¿Quieres preparar la llamada en el marcador habitual?").setPositiveButton("Abrir marcador",(d,n)->{try{startActivity(dial);}catch(Exception error){say("No hay marcador disponible.");}}).setNegativeButton("Cancelar",null).show();return;}catch(Exception e){say("Android no permitió abrir Truecaller.");return;}}try{startActivity(dial);}catch(Exception e){say("No hay marcador disponible.");}}
 private void calendar(){try{Intent i=new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);i.putExtra(CalendarContract.Events.TITLE,"Actividad de Jarvis");startActivity(i);}catch(Exception e){say("No encontré una aplicación de calendario compatible.");}}
 public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED){if(request==1)listen();else if(request==3)activateWake();else if(request==2)findContact(pending,pendingApp);else if(request==4)activateWake();}else say("Permiso rechazado. Puedes seguir usando las otras funciones.");}
 public void onReadyForSpeech(Bundle b){output.setText("Te escucho…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){output.setText("Procesando…");}public void onError(int error){say("No pude escuchar (código "+error+"). Vuelve a pulsar Hablar o escribe.");}public void onResults(Bundle b){ArrayList<String> results=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(results!=null&&!results.isEmpty()){input.setText(results.get(0));command(results.get(0));}}public void onPartialResults(Bundle b){}public void onEvent(int e,Bundle b){}
 protected void onStop(){super.onStop();if(recognizer!=null)recognizer.cancel();new Handler(getMainLooper()).postDelayed(()->{if(!CloudClient.get(this).busy())WakeService.resumeListening();},500);}
 protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.hasExtra("command")){input.setText(intent.getStringExtra("command"));command(intent.getStringExtra("command"));}else if(intent.getBooleanExtra("fromWake",false))listen();}
 private void activateWake(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},3);return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},4);return;}if(recognizer!=null)recognizer.cancel();if(voice!=null)voice.stop();try{startForegroundService(new Intent(this,WakeService.class));getSharedPreferences("setup",0).edit().putBoolean("activated",true).apply();finishAndRemoveTask();}catch(Exception e){say("Android no permitió iniciar la escucha. Actívala con la aplicación abierta.");}}
 protected void onDestroy(){if(recognizer!=null)recognizer.close();if(voice!=null)voice.shutdown();super.onDestroy();}
}

