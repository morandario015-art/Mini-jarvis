package com.dario.jarvis;
import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.*;
import java.util.function.Consumer;

public final class CloudSettings {
 private CloudSettings(){}
 public static void show(Activity activity,Consumer<String> feedback){
  CloudClient cloud=CloudClient.get(activity);
  LinearLayout form=new LinearLayout(activity);form.setOrientation(1);form.setPadding(32,16,32,16);
  TextView info=new TextView(activity);info.setText("Jarvis enviará el texto de tu consulta y hasta cuatro intercambios anteriores al proveedor que elijas. La voz se reconoce en tu teléfono. Jarvis no guarda el chat al cerrarse; Groq y Google aplican sus propias políticas. El plan gratuito de Gemini puede usar el contenido para mejorar sus productos. No envíes datos confidenciales. Las claves de Groq/Gemini se configuran solo en el servidor.");form.addView(info);
  EditText url=new EditText(activity);url.setHint("Dirección HTTPS del servidor");url.setText(cloud.endpoint());url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);form.addView(url);
  EditText token=new EditText(activity);token.setHint("Token personal de Jarvis (no clave de Groq/Gemini)");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);form.addView(token);
  Spinner provider=new Spinner(activity);provider.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,new String[]{"Groq","Gemini"}));provider.setSelection(cloud.provider().equals("gemini")?1:0);form.addView(provider);
  CheckBox consent=new CheckBox(activity);consent.setText("Acepto enviar el texto a este servidor y proveedor. Mis cuentas están en plan gratuito, sin facturación de pago.");form.addView(consent);
  CheckBox actions=new CheckBox(activity);actions.setText("Permitir IA para tareas en otras apps: enviar etiquetas de botones y pistas de campos al proveedor. No se envían contraseñas ni contenido de campos editables.");actions.setChecked(cloud.appActionsEnabled());form.addView(actions);
  ScrollView scroll=new ScrollView(activity);scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Conectar IA a Jarvis").setView(scroll).setPositiveButton("Guardar y comprobar",null).setNegativeButton("Cancelar",null).setNeutralButton("Desconectar IA",(d,n)->{cloud.disable();feedback.accept("IA desconectada. Puedes seguir usando órdenes locales.");}).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   if(!consent.isChecked()){consent.setError("Confirma antes de conectar.");return;}
   try{cloud.configure(url.getText().toString(),token.getText().toString(),provider.getSelectedItemPosition()==0?"groq":"gemini");cloud.setAppActionsEnabled(actions.isChecked());dialog.dismiss();feedback.accept("Comprobando el servidor…");cloud.budget(new CloudClient.Callback(){public void done(String text){if(!activity.isDestroyed())feedback.accept("Conexión comprobada.\n"+text);}public void failed(String text){if(!activity.isDestroyed())feedback.accept(text);}});}catch(Exception e){token.setError(e.getMessage());}
  }));dialog.show();
 }
}
