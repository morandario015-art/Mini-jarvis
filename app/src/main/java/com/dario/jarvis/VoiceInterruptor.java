package com.dario.jarvis;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;

/** Experimental barge-in with Android echo cancellation; requires physical acoustic testing. */
public final class VoiceInterruptor implements AutoCloseable {
 private final Handler main=new Handler(Looper.getMainLooper());
 private Model model;private volatile boolean closed;private volatile int generation;
 private volatile AudioRecord audio;private Thread reader;
 public VoiceInterruptor(Context context){StorageService.unpack(context,"model-es","interrupt-model",m->{if(closed)m.close();else model=m;},e->{});}
 public void start(String spoken,java.util.function.Consumer<String> interrupt){
  stop();if(closed||model==null)return;final int token=generation;
  reader=new Thread(()->{
   AudioRecord local=null;AcousticEchoCanceler echo=null;NoiseSuppressor noise=null;
   try(Recognizer recognizer=new Recognizer(model,16000f)){
    int min=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
    local=new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,8192));
    if(local.getState()!=AudioRecord.STATE_INITIALIZED)return;
    if(AcousticEchoCanceler.isAvailable()){echo=AcousticEchoCanceler.create(local.getAudioSessionId());if(echo!=null)echo.setEnabled(true);}
    if(NoiseSuppressor.isAvailable()){noise=NoiseSuppressor.create(local.getAudioSessionId());if(noise!=null)noise.setEnabled(true);}
    synchronized(this){if(token!=generation||closed)return;audio=local;local.startRecording();}
    String own=Command.clean(spoken);short[] buffer=new short[2048];
    while(!closed&&token==generation){int count=local.read(buffer,0,buffer.length);if(count<=0)break;
     boolean complete=recognizer.acceptWaveForm(buffer,count);
     JSONObject result=new JSONObject(complete?recognizer.getResult():recognizer.getPartialResult());
     String text=Command.clean(result.optString(complete?"text":"partial",""));
     // Ignore likely speaker echo. A direct 'para'/'espera' or a distinct phrase interrupts.
     boolean command=text.equals("para")||text.equals("espera")||text.equals("silencio")||text.equals("jarvis")||text.startsWith("jarvis para");
     if(!text.isEmpty()&&!own.contains(text)&&(command||complete&&text.split(" ").length>=2)){
      main.post(()->{if(!closed&&token==generation){stop();interrupt.accept(text);}});break;
     }
    }
   }catch(Exception ignored){}finally{if(echo!=null)echo.release();if(noise!=null)noise.release();if(local!=null){try{local.stop();}catch(Exception ignored){}local.release();synchronized(this){if(audio==local)audio=null;}}}
  },"jarvis-barge-in");reader.start();
 }
 public synchronized void stop(){generation++;AudioRecord a=audio;if(a!=null)try{a.stop();}catch(Exception ignored){}}
 public void close(){closed=true;stop();Thread t=reader;Model m=model;model=null;if(m!=null)new Thread(()->{try{if(t!=null)t.join(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}if(t==null||!t.isAlive())m.close();},"jarvis-barge-close").start();}
}
