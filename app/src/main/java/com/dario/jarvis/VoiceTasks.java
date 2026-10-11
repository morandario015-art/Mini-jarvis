package com.dario.jarvis;
import android.Manifest;
import android.app.KeyguardManager;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import java.util.*;
import java.util.function.Consumer;

/** Concrete local actions only. Never claims success inside an opaque third-party app. */
public final class VoiceTasks {
 private final Context context;
 private String alarmPending="";
 private boolean dictating;
 private final StringBuilder note=new StringBuilder();
 private Intent dialPending;
 public VoiceTasks(Context c){context=c;}
 private boolean locked(){return ((KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardLocked();}
 public boolean handle(String raw,Consumer<String> reply){
  String text=Command.body(raw);
  if(text.equals("cancelar")||text.equals("cancela")){alarmPending="";dialPending=null;dictating=false;note.setLength(0);reply.accept("Cancelado.");return true;}
  if(dialPending!=null&&(text.equals("si")||text.equals("confirmo")||text.equals("no"))){Intent i=dialPending;dialPending=null;if(text.equals("no"))reply.accept("Llamada cancelada.");else if(locked())reply.accept("Desbloquea para preparar la llamada.");else reply.accept(launch(i)?"Envié el número al marcador. Revisa el contacto antes de llamar.":"Android no permitió abrir el marcador.");return true;}
  if(!alarmPending.isEmpty()&&(text.contains("manana")||text.contains("tarde")||text.contains("noche"))){raw=alarmPending+" de la "+text;alarmPending="";text=Command.body(raw);}
  if(AlarmRequest.isCommand(text)){
   AlarmRequest a=AlarmRequest.parse(raw);if(a==null){reply.accept("Indica la hora, por ejemplo: pon una alarma a las cinco de la mañana todos los días.");return true;}
   if(a.ambiguous){alarmPending=raw;reply.accept("¿A esa hora de la mañana o de la tarde?");return true;}
   if(locked()){reply.accept("Desbloquea el teléfono para configurar el reloj.");return true;}
   Intent i=new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,a.hour).putExtra(AlarmClock.EXTRA_MINUTES,a.minute).putExtra(AlarmClock.EXTRA_MESSAGE,"Alarma de Jarvis").putExtra(AlarmClock.EXTRA_SKIP_UI,true);
   if(a.daily)i.putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS,new ArrayList<>(Arrays.asList(Calendar.SUNDAY,Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY)));
   if(context.getPackageManager().queryIntentActivities(i,0).isEmpty()){reply.accept("No encontré un reloj compatible con alarmas por voz.");return true;}
   reply.accept(launch(i)?"Envié al reloj la alarma para las "+String.format(Locale.ROOT,"%02d:%02d",a.hour,a.minute)+(a.daily?", todos los días.":".")+" La aplicación del reloj debe confirmar que quedó guardada.":"Android no permitió abrir el reloj. Revisa el permiso de acciones por voz en la configuración inicial.");return true;
  }
  if(text.equals("guardar nota")||text.equals("guarda la nota")||text.equals("terminar nota")){
   if(!dictating||note.length()==0){reply.accept("Todavía no hay dictado. Di: toma una nota.");return true;}
   if(locked()){reply.accept("Desbloquea para guardar la nota.");return true;}
   Intent i=new Intent("com.google.android.gms.actions.CREATE_NOTE").putExtra("com.google.android.gms.actions.extra.NAME","Nota dictada con Jarvis").putExtra("com.google.android.gms.actions.extra.TEXT",note.toString());
   List<ResolveInfo> apps=context.getPackageManager().queryIntentActivities(i,0);
   if(apps.isEmpty()){reply.accept("Tu aplicación de notas no admite recibir dictados con esta conexión. El dictado sigue en memoria; di cancelar para borrarlo. Hace falta integrar esa aplicación.");return true;}
   if(apps.size()==1)i.setComponent(new ComponentName(apps.get(0).activityInfo.packageName,apps.get(0).activityInfo.name));
   if(launch(i)){dictating=false;note.setLength(0);reply.accept("Envié el dictado a notas. Si la aplicación pide guardar, confirma allí.");}else reply.accept("Android no permitió abrir notas. El dictado sigue en memoria.");return true;
  }
  if(!dictating&&text.contains("nota")&&(text.startsWith("toma")||text.startsWith("coge")||text.startsWith("crea")||text.startsWith("quiero"))){
   dictating=true;note.setLength(0);reply.accept("Te escucho para tomar la nota. Dicta por partes y di guardar nota al terminar, o cancelar para borrarla.");return true;
  }
  if(dictating){if(note.length()+raw.length()>12000){reply.accept("La nota llegó al límite. Di guardar nota o cancelar.");return true;}note.append(raw).append('\n');reply.accept("Anotado. Continúa o di guardar nota.");return true;}
  Command.Call call=Command.call(raw);
  if(call!=null){prepareCall(call,reply);return true;}
  return false;
 }
 private boolean launch(Intent intent){if(AppActionsService.available())return AppActionsService.launch(intent);try{context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}catch(Exception e){return false;}}
 private void prepareCall(Command.Call call,Consumer<String> reply){
  if(locked()){reply.accept("Desbloquea para preparar la llamada.");return;}
  if(context.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){reply.accept("Falta conceder contactos en la configuración inicial.");return;}
  if(!call.app.isEmpty()&&!call.app.equals("truecaller")){reply.accept("Puedo preparar la llamada en Truecaller o en el marcador.");return;}
  ArrayList<String> labels=new ArrayList<>(),numbers=new ArrayList<>();HashSet<String> seen=new HashSet<>();
  try(Cursor c=context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,null)){
   if(c!=null)while(c.moveToNext()){String name=c.getString(0),number=c.getString(1);if(name!=null&&number!=null&&Command.clean(name).contains(call.name)&&seen.add(number)){labels.add(name);numbers.add(number);}}
  }catch(Exception e){reply.accept("No pude consultar contactos.");return;}
  if(numbers.size()!=1){reply.accept(numbers.isEmpty()?"No encontré una coincidencia exacta. Repite el nombre como está guardado.":"Hay varios números. Repite un nombre más completo para identificar uno.");return;}
  Intent i=new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",numbers.get(0),null));
  if(call.app.equals("truecaller")){i.setPackage("com.truecaller");if(context.getPackageManager().queryIntentActivities(i,0).isEmpty()){reply.accept("Truecaller no admite preparar este número. Pide llamar con el marcador si quieres usarlo.");return;}}
  dialPending=i;reply.accept("Encontré a "+labels.get(0)+". ¿Confirmas que prepare ese número? Di sí o no.");
 }
 public void cancel(){alarmPending="";dialPending=null;dictating=false;note.setLength(0);}
}
