package com.dario.jarvis;
public class CommandTest {public static void main(String[] args){for(String name:new String[]{"true caller","true cooler","tru caller","Truecaller"})if(!Command.appName(name).equals("truecaller"))throw new AssertionError(name);if(!Command.body("Jarvis, abre Truecaller").equals("abre truecaller"))throw new AssertionError("prefix");if(!Command.body("Jarvis, recuerda que mañana tomo café").equals("recuerda que manana tomo cafe"))throw new AssertionError("accents");if(!Command.appName("la aplicación Gmail").equals("gmail"))throw new AssertionError("app prefix");for(String phrase:new String[]{"abre Truecaller e intenta llamar a Karol Carabajo","Jarvis, abre true cooler y llama a Carol Carabajo","abrir Truecaller luego llamar a Carol Carabajo"}){Command.Call c=Command.call(phrase);if(c==null||!c.app.equals("truecaller")||!c.name.endsWith("carabajo"))throw new AssertionError(phrase);}
 if(Command.call("abre Truecaller")!=null)throw new AssertionError("simple app");
 if(Command.call("recuerda que llama a Carol")!=null)throw new AssertionError("note must stay note");
 if(!Command.nearName("Carol Carabajo","karol carabajo"))throw new AssertionError("Carol Karol");
 if(Command.nearName("Carlos Carabajo","karol carabajo"))throw new AssertionError("different contact");
 if(!Command.call("llama a Carol Carabajo").app.isEmpty())throw new AssertionError("default dialer");
 for(String phrase:new String[]{"Jarvis, puedes abrir el WhatsApp por favor","abreme wasap","quiero entrar a whats app"})if(!"whatsapp".equals(Command.openAppQuery(phrase)))throw new AssertionError(phrase);
 if(Command.openAppQuery("Explícame cómo abrir una empresa")!=null)throw new AssertionError("discussion must not open an app");
 if(!Command.isBudget("cuantas consultas quedan"))throw new AssertionError("budget");
 System.out.println("Compound call, names and aliases passed");}}

