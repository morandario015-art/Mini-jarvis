package com.dario.jarvis;
import android.speech.tts.*;
public final class LocalVoice {
 public static boolean select(TextToSpeech voice){java.util.Set<Voice> voices=voice.getVoices();if(voices!=null)for(Voice v:voices)if(v.getLocale().getLanguage().equals("es")&&!v.isNetworkConnectionRequired())return voice.setVoice(v)==TextToSpeech.SUCCESS;return false;}
}
