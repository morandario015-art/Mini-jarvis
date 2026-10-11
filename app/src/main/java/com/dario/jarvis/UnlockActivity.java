package com.dario.jarvis;
import android.app.*;
import android.os.*;
import android.view.WindowManager;
import android.widget.*;
public class UnlockActivity extends Activity {
 public void onCreate(Bundle state){super.onCreate(state);if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
 LinearLayout view=new LinearLayout(this);view.setOrientation(1);view.setPadding(30,70,30,30);TextView text=new TextView(this);text.setText("Jarvis está aquí. Desbloquea con tu método habitual para continuar.");text.setTextSize(22);view.addView(text);Button retry=new Button(this);retry.setText("Desbloquear con Android");retry.setOnClickListener(v->unlock());view.addView(retry);Button cancel=new Button(this);cancel.setText("Cancelar");cancel.setOnClickListener(v->{WakeService.resumeListening();finish();});view.addView(cancel);setContentView(view);unlock();}
 private void unlock(){KeyguardManager guard=getSystemService(KeyguardManager.class);guard.requestDismissKeyguard(this,new KeyguardManager.KeyguardDismissCallback(){public void onDismissSucceeded(){startActivity(new android.content.Intent(UnlockActivity.this,MainActivity.class).putExtra("fromWake",true));finish();}public void onDismissCancelled(){WakeService.resumeListening();finish();}public void onDismissError(){WakeService.resumeListening();}});}
}
