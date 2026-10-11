package com.dario.jarvis;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.net.ssl.HttpsURLConnection;

/** Text-only cloud client. Session history stays in RAM; token is encrypted with Android Keystore. */
public final class CloudClient {
 public interface PlanCallback {void done(JSONObject action);void failed(String text);}
 public boolean appActionsEnabled(){return enabled()&&prefs.getBoolean("appActions",false);}
 public void setAppActionsEnabled(boolean enabled){prefs.edit().putBoolean("appActions",enabled).apply();}
 public interface Callback {void done(String text);void failed(String text);}
 public static final String DEFAULT_URL="https://jarvis-ai-gateway.morandario015.workers.dev";
 private static CloudClient instance;
 public static synchronized CloudClient get(Context c){if(instance==null)instance=new CloudClient(c.getApplicationContext());return instance;}
 private final SharedPreferences prefs;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService executor=Executors.newSingleThreadExecutor();
 private final AtomicBoolean busy=new AtomicBoolean();
 private final ArrayList<JSONObject> history=new ArrayList<>();
 private int generation;
 private String lastAnswer="";
 private CloudClient(Context c){prefs=c.getSharedPreferences("jarvis-cloud",Context.MODE_PRIVATE);}
 public boolean enabled(){return prefs.getBoolean("enabled",false);}
 public boolean busy(){return busy.get();}
 public String endpoint(){return prefs.getString("endpoint",DEFAULT_URL);}
 public String provider(){return prefs.getString("provider","groq");}
 public synchronized String lastAnswer(){return lastAnswer;}
 public synchronized void clear(){generation++;history.clear();lastAnswer="";}
 public void disable(){prefs.edit().putBoolean("enabled",false).remove("secret").remove("iv").apply();clear();}
 public static String validateEndpoint(String value) throws Exception {
  URI u=new URI(value.trim());
  if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null||!(u.getPath()==null||u.getPath().isEmpty()||u.getPath().equals("/")))throw new Exception("Usa la dirección HTTPS del servidor, sin rutas ni parámetros.");
  return new URI("https",null,u.getHost(),u.getPort(),null,null,null).toASCIIString();
 }
 public void configure(String url,String token,String provider) throws Exception {
  if(busy())throw new Exception("Espera a que termine la consulta actual.");
  url=validateEndpoint(url);token=token.trim();
  if(token.length()<32||token.length()>512||token.contains("\n")||token.contains("\r"))throw new Exception("El token personal debe tener entre 32 y 512 caracteres.");
  if(!provider.equals("groq")&&!provider.equals("gemini"))throw new Exception("Proveedor inválido.");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  String secret=Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
  prefs.edit().putString("endpoint",url).putString("provider",provider).putString("secret",secret).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putBoolean("enabled",true).apply();clear();
 }
 private SecretKey key() throws Exception {
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  String alias="jarvis-cloud-token";
  if(store.containsAlias(alias))return (SecretKey)store.getKey(alias,null);
  KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();
 }
 private String token() throws Exception {
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
  return new String(cipher.doFinal(Base64.decode(prefs.getString("secret",""),Base64.NO_WRAP)),StandardCharsets.UTF_8);
 }
 public void chat(String message,Callback callback){run(message,false,null,false,callback);}
 public void plan(String message,JSONObject screen,boolean describe,PlanCallback callback){run(message,false,screen,describe,new Callback(){public void done(String text){try{callback.done(new JSONObject(text));}catch(Exception e){callback.failed("La IA no devolvió una acción válida.");}}public void failed(String text){callback.failed(text);}});}
 public void budget(Callback callback){run("",true,null,false,callback);}
 private void run(String message,boolean budget,JSONObject screen,boolean describe,Callback callback){
  if(!enabled()){callback.failed("Conecta la IA desde Jarvis: Conectar IA. Las órdenes locales siguen disponibles.");return;}
  message=message.trim();if(!budget&&(message.isEmpty()||message.length()>4000)){callback.failed("Escribe un mensaje de hasta 4000 caracteres.");return;}
  if(!busy.compareAndSet(false,true)){callback.failed("Estoy respondiendo a la consulta anterior. Espera unos segundos.");return;}
  final String prompt=message,chosenProvider=provider(),base=endpoint();
  final int session;final JSONArray context=new JSONArray();
  synchronized(this){session=generation;int chars=prompt.length()+(screen==null?0:screen.toString().length());for(JSONObject h:history)chars+=h.optString("content").length();while(!history.isEmpty()&&(history.size()>8||chars>12000)){chars-=history.remove(0).optString("content").length();chars-=history.remove(0).optString("content").length();}for(JSONObject h:history)context.put(h);}
  executor.execute(()->{
   String reply=null,error=null;
   try{
    JSONObject body=null;if(!budget)body=new JSONObject().put("provider",chosenProvider).put("message",prompt).put("history",context);
    if(screen!=null){body.put("mode","action").put("screen",screen).put("describe",describe);}
    JSONObject response=request(base+(budget?"/budget":"/chat"),body);
    if(budget)reply=formatBudget(response);else if(screen!=null){reply=response.getJSONObject("action").toString();}else{
     String answer=response.optString("answer","").trim();if(answer.isEmpty())throw new IOException("La IA no devolvió texto.");
     synchronized(this){if(session==generation){history.add(new JSONObject().put("role","user").put("content",prompt));history.add(new JSONObject().put("role","assistant").put("content",answer));while(history.size()>8){history.remove(0);history.remove(0);}lastAnswer=answer;}}
     reply=answer;
     if(response.optJSONObject("quota")!=null&&response.getJSONObject("quota").optBoolean("warning"))reply+="\n\nAviso: estás llegando al límite interno de consultas.";
    }
   }catch(IOException e){error=e.getMessage();}catch(Exception e){error="No pude conectar la IA. Revisa la conexión y la configuración del servidor.";}
   final String result=reply,failure=error;
   main.post(()->{busy.set(false);synchronized(this){if(session!=generation){callback.failed("La conversación anterior se borró. Puedes iniciar otra.");return;}}if(failure!=null)callback.failed(failure);else callback.done(result);});
  });
 }
 private JSONObject request(String address,JSONObject body) throws Exception {
  HttpsURLConnection connection=(HttpsURLConnection)new java.net.URL(address).openConnection();
  connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(35000);
  connection.setRequestProperty("Authorization","Bearer "+token());connection.setRequestProperty("Accept","application/json");
  try {
   if(body!=null){connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=connection.getOutputStream()){out.write(bytes);}}
   int status=connection.getResponseCode();InputStream stream=status>=200&&status<300?connection.getInputStream():connection.getErrorStream();
   if(stream==null)throw new IOException("El servidor no respondió ("+status+").");
   ByteArrayOutputStream data=new ByteArrayOutputStream();try(InputStream in=stream){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(data.size()+n>65536)throw new IOException("Respuesta demasiado grande.");data.write(buf,0,n);}}
   JSONObject value;try{value=new JSONObject(data.toString("UTF-8"));}catch(Exception e){throw new IOException("El servidor aún no tiene el programa de Jarvis. Completa su configuración.");}
   if(status<200||status>=300)throw new IOException(value.optString("error","Error del servidor ("+status+")."));
   return value;
  }finally{connection.disconnect();}
 }
 private String formatBudget(JSONObject response) throws Exception {
  JSONObject providers=response.getJSONObject("providers");StringBuilder text=new StringBuilder("Presupuesto de Jarvis, hora de Ecuador.\n");
  for(String p:new String[]{"groq","gemini"}){JSONObject q=providers.getJSONObject(p);text.append(p.equals("groq")?"Groq":"Gemini").append(": ").append(q.getInt("dailyUsed")).append(" de ").append(q.getInt("dailyLimit")).append(" hoy; ").append(q.getInt("monthlyUsed")).append(" de ").append(q.getInt("monthlyLimit")).append(" este mes. Sugerencia para el resto de hoy: hasta ").append(q.getInt("recommendedDailyRemaining")).append(" consultas.\n");if(q.optBoolean("warning"))text.append("Aviso: cerca del límite.\n");}
  return text.append("Son límites internos. La cuota real de cada cuenta puede ser menor.").toString();
 }
}
