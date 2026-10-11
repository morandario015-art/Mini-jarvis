package com.dario.jarvis;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
public final class AppInventory {
 private AppInventory(){}
 public static List<String> scan(Context context){
  PackageManager pm=context.getPackageManager();
  Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  TreeSet<String> names=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
  for(ResolveInfo item:pm.queryIntentActivities(intent,0)){
   CharSequence label=item.loadLabel(pm);
   if(label!=null&&!label.toString().trim().isEmpty())names.add(label.toString().trim());
  }
  return new ArrayList<>(names);
 }
 public static int refresh(Context context){
  List<String> names=scan(context);
  JSONArray array=new JSONArray();
  for(String name:names)array.put(name);
  context.getSharedPreferences("app_inventory",Context.MODE_PRIVATE).edit()
   .putString("names",array.toString()).putLong("scanned_at",System.currentTimeMillis()).apply();
  return names.size();
 }
 public static List<String> saved(Context context){
  String json=context.getSharedPreferences("app_inventory",Context.MODE_PRIVATE).getString("names","[]");
  ArrayList<String> names=new ArrayList<>();
  try{JSONArray array=new JSONArray(json);for(int i=0;i<array.length();i++)names.add(array.getString(i));}catch(Exception ignored){}
  return names;
 }
 public static boolean isInventoryCommand(String text){
  return (text.contains("aplicacion")||text.contains("aplicaciones")||text.contains("apps"))
   && (text.contains("reconoc")||text.contains("detect")||text.contains("escane")||text.contains("instalad")||text.contains("lista")||text.contains("inventario")||text.contains("resumen"));
 }
 public static String summary(Context context){
  List<String> names=saved(context);
  if(names.isEmpty())return "Todavía no tengo aplicaciones registradas. Pídeme que reconozca las aplicaciones.";
  StringBuilder b=new StringBuilder("Reconocí "+names.size()+" aplicaciones que Android me permite consultar. ");
  int limit=Math.min(names.size(),15);
  for(int i=0;i<limit;i++){if(i>0)b.append(", ");b.append(names.get(i));}
  if(names.size()>limit)b.append(". Y ").append(names.size()-limit).append(" más.");
  return b.toString();
 }
}