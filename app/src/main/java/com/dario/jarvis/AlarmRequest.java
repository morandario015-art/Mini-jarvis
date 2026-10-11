package com.dario.jarvis;
import java.util.regex.*;
public final class AlarmRequest {
 public final int hour,minute;public final boolean daily,ambiguous;
 private AlarmRequest(int h,int m,boolean d,boolean a){hour=h;minute=m;daily=d;ambiguous=a;}
 public static boolean isCommand(String text){return text.contains("alarma")&&(text.startsWith("pon")||text.startsWith("activa")||text.startsWith("crea")||text.startsWith("programa")||text.startsWith("configura"));}
 public static AlarmRequest parse(String raw){
  String text=Command.body(raw);
  String[] words={"cero","una","dos","tres","cuatro","cinco","seis","siete","ocho","nueve","diez","once","doce","trece","catorce","quince","dieciseis","diecisiete","dieciocho","diecinueve","veinte","veintiuna","veintidos","veintitres"};
  text=text.replaceAll("\\buno\\b","una");for(int i=words.length-1;i>=0;i--)text=text.replaceAll("\\b"+words[i]+"\\b",Integer.toString(i));
  Matcher m=Pattern.compile("(?:a las|para las|a la|para la) (\\d{1,2})(?: (?:y )?(\\d{1,2}|media|cuarto))?").matcher(text);
  if(!m.find())return null;int h=Integer.parseInt(m.group(1)),minute=0;
  if(m.group(2)!=null)minute=m.group(2).equals("media")?30:m.group(2).equals("cuarto")?15:Integer.parseInt(m.group(2));
  if(h>23||minute>59)return null;
  boolean am=text.contains("manana")||text.contains("madrugada"),pm=text.contains("tarde")||text.contains("noche");
  if(pm&&h<12)h+=12;if(am&&h==12)h=0;
  return new AlarmRequest(h,minute,text.contains("todos los dias")||text.contains("todos dias")||text.contains("diaria"),!am&&!pm&&h>=1&&h<=12);
 }
}
