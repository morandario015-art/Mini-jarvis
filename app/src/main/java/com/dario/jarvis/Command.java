package com.dario.jarvis;
import java.text.Normalizer;
import java.util.Locale;
public final class Command {
 public static String clean(String text){ return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+"," ").trim(); }
 public static String body(String text){ return clean(text).replaceFirst("^(hola )?(jarvis|yarvis|jervis) *", ""); }
}
