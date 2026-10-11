package com.dario.jarvis;
import android.content.Context;
import org.vosk.*;
import org.vosk.android.*;
import org.json.JSONObject;
public final class OfflineCommands implements AutoCloseable {
 private final Context context;private Model model;private Recognizer rec;private SpeechService speech;private int generation;private boolean closed,loading;private java.util.function.Consumer<String> nextResult,nextFailure;
 public OfflineCommands(Context c){context=c.getApplicationContext();LibVosk.setLogLevel(LogLevel.WARN);}
 public void start(java.util.function.Consumer<String> result,java.util.function.Consumer<String> failure){cancel();final int token=generation;nextResult=result;nextFailure=failure;if(model==null){if(loading)return;loading=true;StorageService.unpack(context,"model-es","command-model",m->{loading=false;if(closed){m.close();return;}model=m;if(nextResult!=null)listen(generation,nextResult,nextFailure);},e->{loading=false;if(!closed&&nextFailure!=null)nextFailure.accept("No pude preparar la voz local.");});}else listen(token,result,failure);}
 private void listen(int token,java.util.function.Consumer<String> result,java.util.function.Consumer<String> failure){if(closed||token!=generation)return;try{rec=new Recognizer(model,16000f);speech=new SpeechService(rec,16000f);speech.startListening(new RecognitionListener(){private void complete(String json){if(token!=generation||closed)return;try{String text=new JSONObject(json).optString("text","").trim();if(!text.isEmpty()){cancel();result.accept(text);}}catch(Exception e){cancel();failure.accept("No pude interpretar la orden.");}}public void onPartialResult(String s){}public void onResult(String s){complete(s);}public void onFinalResult(String s){complete(s);if(token==generation){cancel();failure.accept("No escuché una orden. Di Jarvis otra vez.");}}public void onError(Exception e){if(token==generation){cancel();failure.accept("No pude usar el micrófono.");}}public void onTimeout(){if(token==generation){cancel();failure.accept("No escuché una orden. Di Jarvis otra vez.");}}},12000);}catch(Exception e){cancel();failure.accept("No pude iniciar la voz local.");}}
 public void cancel(){generation++;nextResult=null;nextFailure=null;if(speech!=null){speech.cancel();speech.shutdown();speech=null;}if(rec!=null){rec.close();rec=null;}}
 public void close(){closed=true;cancel();if(model!=null){model.close();model=null;}}
}
