package com.masten.assistant;
import android.service.notification.NotificationListenerService;import android.service.notification.StatusBarNotification;import android.speech.tts.TextToSpeech;import android.os.*;import java.util.*;
public class MastenNotificationListener extends NotificationListenerService{
 TextToSpeech tts;
 public void onCreate(){super.onCreate();tts=new TextToSpeech(this,s->{if(s==0)tts.setLanguage(new Locale("fa","IR"));});}
 // کلمات حساس که نباید با صدای بلند خوانده شوند (کد تأیید، رمز عبور و...)
 static final String[] SENSITIVE=new String[]{"رمز","کد تایید","کد تأیید","otp","password","verification","پین","cvv"};
 public void onNotificationPosted(StatusBarNotification s){if(s==null||s.getNotification()==null)return;CharSequence title=s.getNotification().extras.getCharSequence("android.title");CharSequence text=s.getNotification().extras.getCharSequence("android.text");if(text!=null&&title!=null){
  String lower=(title+" "+text).toString().toLowerCase(new Locale("fa"));
  for(String w:SENSITIVE){if(lower.contains(w)){return;}} // این نوع اعلان بلند خوانده نمی‌شود
  String msg="پیام جدید از "+title+": "+text;tts.speak(msg,TextToSpeech.QUEUE_FLUSH,null,"notify");}}
 public void onDestroy(){if(tts!=null)tts.shutdown();super.onDestroy();}
}
