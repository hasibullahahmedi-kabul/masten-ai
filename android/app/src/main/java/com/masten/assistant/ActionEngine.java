package com.masten.assistant;
import android.content.*;import android.net.Uri;import android.provider.Settings;import java.util.*;
public class ActionEngine{
 public static String execute(Context c,String q){String x=q.toLowerCase(new Locale("fa"));
  if(x.contains("تنظیمات")) {c.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return "تنظیمات باز شد.";}
  if(x.contains("تماس")||x.contains("زنگ")){String num=q.replaceAll("[^0-9+]","");if(num.length()>4){c.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+num)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return "صفحه تماس باز شد؛ برای شماره‌گیری خودت تأیید کن.";}return "شماره تماس را مشخص کن.";}
  if(x.contains("youtube")){open(c,"com.google.android.youtube");return "YouTube باز شد.";}
  if(x.contains("تلگرام")){open(c,"org.telegram.messenger");return "Telegram باز شد.";}
  if(x.contains("واتساپ")){open(c,"com.whatsapp");return "WhatsApp باز شد.";}
  if(x.contains("فیلم")||x.contains("ویدیو")||x.contains("آهنگ")||x.contains("موسیقی")){Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse("content://media/external/video/media"),"video/*");i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{c.startActivity(i);return "پخش‌کننده رسانه باز شد.";}catch(Exception e){return "پخش‌کننده مناسب پیدا نشد.";}}
  if(x.contains("اینستاگرام")){open(c,"com.instagram.android");return "Instagram باز شد.";}
  if(x.contains("پیامک")||x.contains("اس ام اس")||x.contains("sms")){
   String body=extractAfter(q,new String[]{"پیامک","اس ام اس","sms"});
   Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"));
   i.putExtra("sms_body",body.isEmpty()?q:body);
   i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   try{c.startActivity(i);return "برنامه پیامک با متن آماده باز شد؛ گیرنده و دکمه ارسال را خودت در همان برنامه تأیید کن.";}catch(Exception e){return "برنامه پیامک پیدا نشد.";}
  }
  if(x.contains("پیام بفرست")||x.contains("پست کن")||x.contains("اشتراک")||x.contains("share")){
   String body=extractAfter(q,new String[]{"پیام بفرست","پست کن","اشتراک بذار","اشتراک‌گذاری","share"});
   Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,body.isEmpty()?q:body);
   try{
    Intent chooser=Intent.createChooser(i,"ارسال با...");chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    c.startActivity(chooser);
    return "صفحه انتخاب برنامه (واتساپ/تلگرام/اینستاگرام/ایمیل و...) باز شد؛ گیرنده و ارسال نهایی را خودت در همان برنامه تأیید کن.";
   }catch(Exception e){return "برنامه‌ای برای اشتراک‌گذاری پیدا نشد.";}
  }
  return "دستور دریافت شد. برای اجرای هوش مصنوعی محلی، Backend/Ollama را فعال کن.";
 }
 static void open(Context c,String p){try{Intent i=c.getPackageManager().getLaunchIntentForPackage(p);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(i);}}catch(Exception ignored){}}
 // متن بعد از عبارت کلیدی را برمی‌گرداند (مثلا "پیام بفرست سلام خوبی؟" -> "سلام خوبی؟")
 static String extractAfter(String q,String[] keys){
  String lower=q.toLowerCase(new Locale("fa"));
  for(String k:keys){int idx=lower.indexOf(k);if(idx>=0){String rest=q.substring(idx+k.length()).trim();if(!rest.isEmpty())return rest;}}
  return "";
 }
}
