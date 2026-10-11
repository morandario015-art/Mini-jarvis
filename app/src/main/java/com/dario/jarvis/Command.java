package com.dario.jarvis;
import java.text.Normalizer;
import java.util.Locale;
public final class Command {
 public static String clean(String text){ return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+"," ").trim(); }
 public static String appName(String text){String name=clean(text).replaceFirst("^(la )?(aplicacion |app )?","");String compact=name.replace(" ","");if(compact.equals("truecaller")||compact.equals("truecooler")||compact.equals("trucaller")||compact.equals("trucoler")||compact.equals("trucoler"))return "truecaller";return name;}
 public static String canonicalApp(String text){
  String name=appName(text);
  if(name.equals("hoja de calculo")||name.equals("hojas de calculo")||name.equals("hojas de calculos")||name.equals("google sheet")||name.equals("sheets")||name.equals("hojas de google"))return "google sheets";
  if(name.equals("documento")||name.equals("documentos")||name.equals("documentos de google")||name.equals("google doc")||name.equals("docs"))return "google docs";
  if(name.equals("presentacion")||name.equals("presentaciones")||name.equals("diapositivas")||name.equals("google slide")||name.equals("slides"))return "google slides";
  if(name.equals("excel")||name.equals("microsof excel"))return "excel";
  if(name.equals("power point")||name.equals("pauer point"))return "powerpoint";
  if(name.equals("wasap")||name.equals("guasap")||name.equals("whats app"))return "whatsapp";
  return name;
 }
 public static boolean fuzzyApp(String spoken,String installed){
  String a=canonicalApp(spoken),b=clean(installed);
  if(a.equals(b)||b.contains(a)||a.contains(b)&&b.length()>3)return true;
  return a.length()>=5&&b.length()>=5&&distance(a.replace(" ",""),b.replace(" ",""))<=2;
 }
 public static final class Call {public final String app,name;Call(String a,String n){app=a;name=n;}}
 public static Call call(String raw){String text=body(raw);java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(?:abre|abrir) (.+?) (?:y|e|luego|despues|y luego|y despues) (?:intenta |intentar )?(?:llama|llamar) a (.+)$").matcher(text);if(m.matches())return new Call(appName(m.group(1)),m.group(2));m=java.util.regex.Pattern.compile("^(?:intenta )?(?:llama|llamar)(?: a)? (.+)$").matcher(text);return m.matches()?new Call("",m.group(1)):null;}
 public static boolean nearName(String saved,String spoken){String a=clean(saved),b=clean(spoken);if(a.contains(b))return true;if(b.length()<4)return false;String[] wanted=b.split(" "),actual=a.split(" ");for(String w:wanted){boolean found=false;for(String s:actual){if(w.equals(s)||w.length()>=4&&s.length()>=4&&distance(w,s)<=1){found=true;break;}}if(!found)return false;}return true;}
 private static int distance(String a,String b){int[] row=new int[b.length()+1];for(int j=0;j<row.length;j++)row[j]=j;for(int i=1;i<=a.length();i++){int prev=row[0];row[0]=i;for(int j=1;j<=b.length();j++){int old=row[j];row[j]=Math.min(Math.min(row[j]+1,row[j-1]+1),prev+(a.charAt(i-1)==b.charAt(j-1)?0:1));prev=old;}}return row[b.length()];}
 public static String body(String text){ return clean(text).replaceFirst("^(hola )?(jarvis|yarvis|jervis) *", ""); }
}
