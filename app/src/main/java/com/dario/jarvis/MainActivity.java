package com.dario.jarvis;
import android.Manifest;
import android.app.*;
import android.os.Bundle;
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
 private TextView output; private EditText input; private SpeechRecognizer recognizer; private TextToSpeech voice; private boolean voiceReady=false; private String pending="";
 public void onCreate(Bundle state){super.onCreate(state);
 LinearLayout root=new LinearLayout(this); root.setOrientation(1);root.setPadding(28,40,28,24);root.setBackgroundColor(0xff081725);
 ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 TextView title=new TextView(this);title.setText("JARVIS · ANDROID");title.setTextSize(28);title.setTextColor(0xff77deef);root.addView(title);
 TextView description=new TextView(this);description.setText("Abre aplicaciones · Prepara llamadas · Guarda recuerdos");description.setTextColor(0xffe1edf5);root.addView(description);
 Button talk=new Button(this);talk.setText("Hablar");root.addView(talk);talk.setOnClickListener(v->listen());
 input=new EditText(this);input.setHint("Jarvis, abre Truecaller");input.setTextColor(-1);input.setHintTextColor(0xffa0bacb);root.addView(input);
 Button send=new Button(this);send.setText("Enviar");root.addView(send);send.setOnClickListener(v->command(input.getText().toString()));
 Button summary=new Button(this);summary.setText("Mis recuerdos");root.addView(summary);summary.setOnClickListener(v->command("mi resumen"));
 Button calendar=new Button(this);calendar.setText("Crear recordatorio en calendario");root.addView(calendar);calendar.setOnClickListener(v->calendar());
 output=new TextView(this);output.setTextSize(18);output.setTextColor(-1);output.setPadding(0,24,0,24);root.addView(output);output.setText("Pulsa Hablar para comenzar. Gmail, IA y análisis XTB todavía no están conectados.");
 voice=new TextToSpeech(this,status->{voiceReady=status==TextToSpeech.SUCCESS;if(voiceReady)voice.setLanguage(new Locale("es","EC"));});
 if(SpeechRecognizer.isRecognitionAvailable(this)){recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(this);}
 }
 private void say(String text){output.setText(text);if(voiceReady)voice.speak(text,TextToSpeech.QUEUE_FLUSH,null,"jarvis");}
 private void listen(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;}if(recognizer==null){say("No hay un servicio de reconocimiento de voz disponible. Puedes escribir.");return;}voice.stop();Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-EC");try{recognizer.startListening(i);}catch(Exception e){say("No pude iniciar el micrófono. Intenta otra vez.");}}
 private void command(String raw){String text=Command.body(raw);if(text.startsWith("abrir "))text="abre "+text.substring(6);if(text.startsWith("abre ")){openApp(text.substring(5));return;}if(text.startsWith("llama a ")){findContact(text.substring(8));return;}if(text.startsWith("llama ")){findContact(text.substring(6));return;}if(text.startsWith("recuerda que ")||text.startsWith("guarda que ")){String memory=text.substring(text.indexOf("que ")+4).trim();if(memory.isEmpty()){say("¿Qué quieres recordar?");return;}String previous=getPreferences(0).getString("mem","");getPreferences(0).edit().putString("mem",previous+memory+"\n").apply();say("Guardé: "+memory+". Esto es una nota; para recibir un aviso usa el calendario.");return;}if(text.contains("resumen")){String mem=getPreferences(0).getString("mem","");say(mem.isEmpty()?"Aún no tienes recuerdos guardados.":"Tus recuerdos:\n"+mem);return;}if(text.contains("recordatorio")){calendar();return;}say("Puedes decir: abre Truecaller, llama a un contacto, recuerda que mañana tomo café o mi resumen. La conversación con IA todavía no está conectada.");}
 private void openApp(String query){query=Command.appName(query);if(query.trim().isEmpty()){say("Indica el nombre de la aplicación.");return;}Intent launcher=new Intent(Intent.ACTION_MAIN);launcher.addCategory(Intent.CATEGORY_LAUNCHER);List<ResolveInfo> all=getPackageManager().queryIntentActivities(launcher,0);List<ResolveInfo> matches=new ArrayList<>();List<String> labels=new ArrayList<>();for(ResolveInfo r:all){String label=r.loadLabel(getPackageManager()).toString();String name=Command.clean(label);if(name.equals(query)||name.contains(query)){matches.add(r);labels.add(label);}}if(matches.isEmpty()){say("No encontré una aplicación llamada "+query+".");return;}if(matches.size()==1){launch(matches.get(0));return;}say("Hay varias aplicaciones. Elige una en pantalla.");new AlertDialog.Builder(this).setTitle("¿Qué aplicación?").setItems(labels.toArray(new String[0]),(d,n)->launch(matches.get(n))).setNegativeButton("Cancelar",null).show();}
 private void launch(ResolveInfo app){Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setComponent(new ComponentName(app.activityInfo.packageName,app.activityInfo.name));try{startActivity(i);}catch(Exception e){say("Android no permitió abrir esa aplicación.");}}
 private void findContact(String name){if(name.trim().isEmpty()){say("Indica el nombre del contacto.");return;}if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){pending=name;requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},2);return;}List<String> labels=new ArrayList<>(),numbers=new ArrayList<>();Set<String> unique=new HashSet<>();String[] projection={ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};try(Cursor c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,projection,null,null,null)){if(c!=null)while(c.moveToNext()){String label=c.getString(0),number=c.getString(1);if(label!=null&&number!=null&&Command.clean(label).contains(name)&&unique.add(label+number)){labels.add(label+" · "+number);numbers.add(number);}}}catch(Exception e){say("No pude consultar tus contactos.");return;}if(numbers.isEmpty()){say("No encontré ese contacto. Prueba con su nombre como está guardado.");return;}say("Elige el número para abrir el marcador.");new AlertDialog.Builder(this).setTitle("Llamar a "+name).setItems(labels.toArray(new String[0]),(d,n)->{try{startActivity(new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",numbers.get(n),null)));}catch(Exception e){say("No hay marcador disponible.");}}).setNegativeButton("Cancelar",null).show();}
 private void calendar(){try{Intent i=new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);i.putExtra(CalendarContract.Events.TITLE,"Actividad de Jarvis");startActivity(i);}catch(Exception e){say("No encontré una aplicación de calendario compatible.");}}
 public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED){if(request==1)listen();else if(request==2)findContact(pending);}else say("Permiso rechazado. Puedes seguir usando las otras funciones.");}
 public void onReadyForSpeech(Bundle b){output.setText("Te escucho…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){output.setText("Procesando…");}public void onError(int error){say("No pude escuchar (código "+error+"). Vuelve a pulsar Hablar o escribe.");}public void onResults(Bundle b){ArrayList<String> results=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(results!=null&&!results.isEmpty()){input.setText(results.get(0));command(results.get(0));}}public void onPartialResults(Bundle b){}public void onEvent(int e,Bundle b){}
 protected void onStop(){super.onStop();if(recognizer!=null)recognizer.cancel();}
 protected void onDestroy(){if(recognizer!=null)recognizer.destroy();if(voice!=null)voice.shutdown();super.onDestroy();}
}
