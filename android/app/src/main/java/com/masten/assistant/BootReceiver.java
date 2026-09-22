package com.masten.assistant;
import android.content.*;import android.os.*;
public class BootReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){ if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())||Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())){Intent s=new Intent(c,MastenService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s);else c.startService(s);}}}
