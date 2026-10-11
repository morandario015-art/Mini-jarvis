package com.dario.jarvis;
public class AlarmRequestTest {
 public static void main(String[] args){
  AlarmRequest a=AlarmRequest.parse("Jarvis, pon la alarma a las cinco de la mañana todos los días");
  if(a==null||a.hour!=5||a.minute!=0||!a.daily||a.ambiguous)throw new AssertionError("daily 5am");
  a=AlarmRequest.parse("pon una alarma a las cinco y media de la tarde");if(a.hour!=17||a.minute!=30||a.ambiguous)throw new AssertionError("17:30");
  a=AlarmRequest.parse("pon la alarma a las 5:30 de la mañana");if(a.hour!=5||a.minute!=30)throw new AssertionError("numeric time");
  a=AlarmRequest.parse("pon la alarma a las cinco");if(!a.ambiguous)throw new AssertionError("must clarify am pm");
  if(AlarmRequest.parse("pon alarma a las 25 de la mañana")!=null)throw new AssertionError("bad hour");
  System.out.println("Alarm hour, recurrence and clarification passed");
 }
}
