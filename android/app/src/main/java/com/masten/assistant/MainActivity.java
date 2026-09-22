package com.masten.assistant;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    TextToSpeech tts; EditText input; EditText backendUrlField; TextView output; Button ask;
    static final int SPEECH=9001; static final int PICK_VIDEO=9002;
    SharedPreferences prefs;
    @Override public void onCreate(Bundle b){ super.onCreate(b); prefs=getSharedPreferences("masten",MODE_PRIVATE); setContentView(build());
        tts=new TextToSpeech(this,this); requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS},42);
        startService(new Intent(this,MastenService.class));
    }
    View build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,28); root.setBackgroundColor(0xff090909);
        TextView title=new TextView(this); title.setText("MASTEN AI"); title.setTextSize(28); title.setTextColor(0xff00e5ff); root.addView(title);
        TextView sub=new TextView(this); sub.setText("Offline-first Persian assistant"); sub.setTextColor(0xffaaaaaa); root.addView(sub);
        backendUrlField=new EditText(this); backendUrlField.setHint("آدرس Backend، مثلا http://192.168.1.10:5000");
        backendUrlField.setText(prefs.getString("backend_url","http://192.168.1.10:5000"));
        backendUrlField.setTextColor(0xffffffff); backendUrlField.setHintTextColor(0xff777777); backendUrlField.setTextSize(12);
        backendUrlField.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable e){prefs.edit().putString("backend_url",e.toString().trim()).apply();}
            public void beforeTextChanged(CharSequence s,int a,int c2,int d){} public void onTextChanged(CharSequence s,int a,int b2,int c2){}
        });
        root.addView(backendUrlField,new LinearLayout.LayoutParams(-1,-2));
        input=new EditText(this); input.setHint("دستور خود را بنویسید..."); input.setTextColor(0xffffffff); input.setHintTextColor(0xff777777); root.addView(input,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(this); ask=new Button(this); ask.setText("اجرا"); Button mic=new Button(this); mic.setText("🎙 صدا"); Button translate=new Button(this); translate.setText("🌐 ترجمه"); row.addView(ask,new LinearLayout.LayoutParams(0,-2,1)); row.addView(mic,new LinearLayout.LayoutParams(0,-2,1)); row.addView(translate,new LinearLayout.LayoutParams(0,-2,1)); root.addView(row);
        LinearLayout row2=new LinearLayout(this); Button settings=new Button(this); settings.setText("مجوزها"); Button dub=new Button(this); dub.setText("🎬 دوبله فیلم/سریال"); row2.addView(settings,new LinearLayout.LayoutParams(0,-2,1)); row2.addView(dub,new LinearLayout.LayoutParams(0,-2,1)); root.addView(row2);
        output=new TextView(this); output.setTextColor(0xffffffff); output.setTextSize(16); output.setPadding(0,24,0,0); root.addView(output,new LinearLayout.LayoutParams(-1,0,1));
        ask.setOnClickListener(v->handle(input.getText().toString())); mic.setOnClickListener(v->listen()); translate.setOnClickListener(v->pickLanguageAndTranslate()); settings.setOnClickListener(v->openNotificationAccess()); dub.setOnClickListener(v->pickVideoForDubbing());
        return root;
    }
    void pickVideoForDubbing(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*");
        try{startActivityForResult(i,PICK_VIDEO);}catch(Exception e){output.setText("انتخاب‌گر فایل در دسترس نیست.");}
    }
    void startDubbing(Uri videoUri){
        String[] names=TranslateHelper.languageNames();
        new AlertDialog.Builder(this)
            .setTitle("دوبله به کدام زبان؟")
            .setItems(names,(dialog,which)->{
                String targetName=names[which];
                String targetCode=TranslateHelper.codeForName(targetName);
                String backend=backendUrlField.getText().toString().trim();
                if(backend.isEmpty()){output.setText("اول آدرس Backend را وارد کن.");return;}
                output.setText("شروع دوبله به "+targetName+"...\nاین کار روی Backend (کامپیوتر) انجام می‌شود و ممکن است چند دقیقه طول بکشد.");
                new DubbingClient().dub(this,backend,videoUri,targetCode,new DubbingClient.Listener(){
                    @Override public void onProgress(String message){runOnUiThread(()->output.setText(message));}
                    @Override public void onDone(java.io.File outputFile){runOnUiThread(()->{String msg="دوبله تمام شد ✅\nمسیر فایل: "+outputFile.getAbsolutePath();output.setText(msg);speak("دوبله تمام شد.");});}
                    @Override public void onError(String message){runOnUiThread(()->output.setText("خطا: "+message));}
                });
            }).show();
    }
    void pickLanguageAndTranslate(){
        String text=input.getText().toString();
        if(text.trim().isEmpty()){output.setText("اول متنی برای ترجمه بنویس.");return;}
        String[] names=TranslateHelper.languageNames();
        new AlertDialog.Builder(this)
            .setTitle("ترجمه به کدام زبان؟")
            .setItems(names,(dialog,which)->{
                String target=names[which];
                output.setText("در حال ترجمه به "+target+"...");
                TranslateHelper.translateAuto(text,target,new TranslateHelper.Callback(){
                    @Override public void onResult(String translatedText){runOnUiThread(()->{output.setText(translatedText);speak(translatedText);});}
                    @Override public void onError(String message){runOnUiThread(()->output.setText("خطا: "+message));}
                });
            }).show();
    }
    void handle(String q){ if(q.trim().isEmpty())return; String r=ActionEngine.execute(this,q); output.setText(r); speak(r); }
    void listen(){ Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR"); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); try{startActivityForResult(i,SPEECH);}catch(Exception e){output.setText("تشخیص صدا در این دستگاه در دسترس نیست.");} }
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);
        if(r==SPEECH&&c==RESULT_OK&&d!=null){ArrayList<String>x=d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(x!=null&&!x.isEmpty()){input.setText(x.get(0));handle(x.get(0));}}
        if(r==PICK_VIDEO&&c==RESULT_OK&&d!=null&&d.getData()!=null){startDubbing(d.getData());}
    }
    void speak(String s){if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"masten");}
    public void onInit(int status){if(tts!=null)tts.setLanguage(new Locale("fa","IR"));}
    void openNotificationAccess(){startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));}
    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
