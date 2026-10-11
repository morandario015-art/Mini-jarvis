package com.dario.jarvis;
import android.content.Context;
import org.vosk.*;
import org.vosk.android.*;
import org.json.JSONObject;
import java.io.IOException;
public final class OfflineWakeDetector implements AutoCloseable {
 private final Context context;private final Runnable ready,wake;private final java.util.function.Consumer<Exception> error;
 private Model model;private Recognizer recognizer;private SpeechService speech;private boolean closed=false;private int generation=0;
 public OfflineWakeDetector(Context c,Runnable r,Runnable w,java.util.function.Consumer<Exception> e){context=c.getApplicationContext();ready=r;wake=w;error=e;LibVosk.setLogLevel(LogLevel.WARNINGS);}
 public void load(){StorageService.unpack(context,"model-en-us","wake-model",m->{if(closed){m.close();return;}model=m;ready.run();},e->{if(!closed)error.accept(e);});}
 public void start() throws IOException {if(closed||model==null||speech!=null)return;final int token=++generation;recognizer=new Recognizer(model,16000f,"[\"jarvis\",\"[unk]\"]");speech=new SpeechService(recognizer,16000f);speech.startListening(new RecognitionListener(){private void check(String json){if(closed||token!=generation)return;try{JSONObject obj=new JSONObject(json);String text=obj.optString("text",obj.optString("partial",""));if(text.trim().equals("jarvis"))wake.run();}catch(Exception ignored){}}public void onPartialResult(String s){check(s);}public void onResult(String s){check(s);}public void onFinalResult(String s){}public void onError(Exception e){if(token==generation&&!closed)error.accept(e);}public void onTimeout(){}});}
 public void stop(){generation++;if(speech!=null){speech.cancel();speech.shutdown();speech=null;}if(recognizer!=null){recognizer.close();recognizer=null;}}
 public void close(){closed=true;stop();if(model!=null){model.close();model=null;}}
}
