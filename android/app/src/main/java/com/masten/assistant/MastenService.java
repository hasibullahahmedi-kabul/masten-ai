package com.masten.assistant;
import android.app.*;import android.content.*;import android.os.*;
public class MastenService extends Service{
 static final String CH="masten_service";
 @Override public void onCreate(){super.onCreate();NotificationChannel ch=new NotificationChannel(CH,"Masten AI",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(ch);Notification n=new Notification.Builder(this,CH).setContentTitle("Masten AI").setContentText("دستیار فعال است").setSmallIcon(android.R.drawable.ic_dialog_info).build();startForeground(77,n);}
 @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 @Override public IBinder onBind(Intent i){return null;}
}
