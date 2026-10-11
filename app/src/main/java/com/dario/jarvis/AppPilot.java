package com.dario.jarvis;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.function.Consumer;

/** Bounded AI planner over accessible controls, with stale-screen and sensitive-action checks. */
public final class AppPilot {
 private final Context context;private final Handler handler=new Handler(Looper.getMainLooper());
 private String target="",appName="",task="";private int steps,generation;
 private JSONObject pendingAction;private Screen pendingScreen;
 private Consumer<String> reply;
 private static final class Screen {String fingerprint;JSONObject json;ArrayList<AccessibilityNodeInfo> nodes=new ArrayList<>();void close(){for(AccessibilityNodeInfo n:nodes)n.recycle();}}
 public AppPilot(Context c){context=c;}
 public boolean active(){return !target.isEmpty();}
 public void cancel(){generation++;target="";task="";pendingAction=null;if(pendingScreen!=null){pendingScreen.close();pendingScreen=null;}}
 public void opened(String packageName,String label,Consumer<String> feedback){cancel();target=packageName;appName=label;reply=feedback;handler.postDelayed(()->{if(active())run("Explica brevemente qué acciones se pueden hacer según los controles visibles de esta app y pregunta qué necesito. No ejecutes acciones.",true);},900);}
 public boolean handle(String raw,Consumer<String> feedback){
  if(!active())return false;reply=feedback;String text=Command.body(raw);
  if(pendingAction!=null){if(text.equals("si")||text.equals("confirmo")){JSONObject action=pendingAction;Screen screen=pendingScreen;pendingAction=null;pendingScreen=null;perform(action,screen,true);return true;}if(text.equals("no")||text.equals("cancelar")){pendingAction=null;if(pendingScreen!=null)pendingScreen.close();pendingScreen=null;reply.accept("Acción cancelada.");return true;}reply.accept("Hay una acción pendiente. Di confirmo o cancelar.");return true;}
  generation++;steps=0;task=raw;run(raw,false);return true;
 }
 private boolean locked(){return ((KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardLocked();}
 private Screen capture(){
  AppActionsService service=AppActionsService.active();if(service==null||locked())return null;
  AccessibilityNodeInfo root=service.getRootInActiveWindow();if(root==null)return null;
  try{
   if(root.getPackageName()==null||!target.equals(root.getPackageName().toString()))return null;
   if(target.contains("permissioncontroller")||target.contains("packageinstaller")||target.equals("com.android.systemui"))return null;
   Screen screen=new Screen();JSONArray controls=new JSONArray();collect(root,screen,controls,0,new int[]{0});
   screen.json=new JSONObject().put("app",appName).put("package",target).put("controls",controls);screen.fingerprint=screen.json.toString();return screen;
  }catch(Exception e){return null;}finally{root.recycle();}
 }
 private void collect(AccessibilityNodeInfo node,Screen screen,JSONArray controls,int depth,int[] visited)throws Exception{
  if(depth>20||visited[0]++>500||controls.length()>=50||!node.isVisibleToUser())return;
  if(node.isPassword())return;
  boolean editable=node.isEditable(),usable=node.isEnabled()&&(node.isClickable()||editable||node.isScrollable());
  String label="";
  if(editable){if(node.getHintText()!=null)label=node.getHintText().toString();else if(node.getContentDescription()!=null)label=node.getContentDescription().toString();}
  else if(node.getText()!=null)label=node.getText().toString();else if(node.getContentDescription()!=null)label=node.getContentDescription().toString();
  if(label.length()>120)label=label.substring(0,120);
  if(usable){Rect bounds=new Rect();node.getBoundsInScreen(bounds);int id=screen.nodes.size();screen.nodes.add(AccessibilityNodeInfo.obtain(node));controls.put(new JSONObject().put("id",id).put("label",label).put("editable",editable).put("clickable",node.isClickable()).put("scrollable",node.isScrollable()).put("viewId",node.getViewIdResourceName()==null?"":node.getViewIdResourceName()).put("bounds",bounds.flattenToString()));}
  for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){try{collect(child,screen,controls,depth+1,visited);}finally{child.recycle();}}}
 }
 private void run(String message,boolean describe){
  if(!CloudClient.get(context).appActionsEnabled()){reply.accept("Para interpretar controles, activa acciones con IA en Conectar IA. La app quedó abierta.");return;}
  if(++steps>5){reply.accept("Detuve la tarea tras cinco pasos para reservar presupuesto. Dime cómo continuar.");return;}
  Screen screen=capture();if(screen==null){reply.accept("No puedo ver controles de la app solicitada. Puede requerir desbloqueo, Accesibilidad o una integración específica.");return;}
  if(screen.nodes.isEmpty()){screen.close();reply.accept("Esta pantalla no expone controles compatibles. No puedo automatizarla todavía.");return;}
  int token=generation;
  CloudClient.get(context).plan(message,screen.json,describe,new CloudClient.PlanCallback(){
   public void done(JSONObject action){if(token!=generation){screen.close();return;}if(describe){screen.close();reply.accept(action.optString("message","¿Qué necesitas hacer?"));return;}perform(action,screen,false);}
   public void failed(String error){screen.close();if(token==generation)reply.accept(error);}
  });
 }
 private void perform(JSONObject action,Screen old,boolean confirmed){
  Screen current=capture();if(current==null||!old.fingerprint.equals(current.fingerprint)){old.close();if(current!=null)current.close();reply.accept("La pantalla cambió. Repite la tarea para revisarla antes de actuar.");return;}current.close();
  String type=action.optString("type"),message=action.optString("message");int id=action.optInt("id",-1);
  if(type.equals("ask")){old.close();reply.accept(message);return;}
  if(type.equals("done")){try{String evidence=old.json.getJSONArray("controls").getJSONObject(id).optString("label");old.close();reply.accept("La aplicación muestra: "+evidence+". "+message);}catch(Exception e){old.close();reply.accept("No pude confirmar que la tarea terminó. Revisa la aplicación.");}return;}
  if(id<0||id>=old.nodes.size()){old.close();reply.accept("La IA no identificó un control válido.");return;}
  AccessibilityNodeInfo node=old.nodes.get(id);String label="";try{label=old.json.getJSONArray("controls").getJSONObject(id).optString("label");}catch(Exception ignored){}
  String normalized=Command.clean(label);
  boolean sensitive=normalized.matches(".*\\b(enviar|send|pagar|pay|comprar|buy|transferir|borrar|delete|eliminar|desinstalar|publicar|publish|confirmar|confirm|aceptar|accept|suscribir|subscribe)\\b.*");
  if(normalized.matches(".*\\b(permitir|allow|conceder|grant)\\b.*")){old.close();reply.accept("Los permisos de Android debes concederlos personalmente.");return;}
  if(!confirmed&&type.equals("click")&&sensitive){pendingAction=action;pendingScreen=old;reply.accept("La tarea pide pulsar "+label+". ¿Lo confirmas? Di confirmo o cancelar.");return;}
  boolean accepted=false;
  if(type.equals("click")&&node.isClickable())accepted=node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
  if(type.equals("type")&&node.isEditable()&&!node.isPassword()){Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,action.optString("text"));accepted=node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);}
  if(type.equals("scroll")&&node.isScrollable())accepted=node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
  old.close();
  if(!accepted){reply.accept("La app no aceptó el control solicitado. No puedo confirmar el resultado.");return;}
  int token=generation;handler.postDelayed(()->{if(token==generation)run(task,false);},1000);
 }
}
