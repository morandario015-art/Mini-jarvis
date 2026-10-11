package com.dario.jarvis;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

/** User-enabled Android bridge for launching requested app intents from a voice service.
 * AppPilot inspects accessible controls only for the explicitly selected application.
 */
public class AppActionsService extends AccessibilityService {
 private static AppActionsService active;
 protected void onServiceConnected(){active=this;}
 public void onAccessibilityEvent(AccessibilityEvent event){}
 public void onInterrupt(){}
 public void onDestroy(){if(active==this)active=null;super.onDestroy();}
 public static AppActionsService active(){return active;}
 public static boolean available(){return active!=null;}
 public static boolean launch(Intent intent){if(active==null)return false;try{active.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;}catch(Exception e){return false;}}
}
