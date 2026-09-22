package com.masten.assistant;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ترجمه چندزبانه روی خود دستگاه (بعد از اولین دانلود مدل هر زبان، آفلاین کار می‌کند).
 * زبان مبدا به‌صورت خودکار تشخیص داده می‌شود؛ زبان مقصد را کاربر انتخاب می‌کند.
 */
public class TranslateHelper {

    public interface Callback {
        void onResult(String translatedText);
        void onError(String message);
    }

    // نام فارسی زبان -> کد ML Kit. هر زبان جدید را همین‌جا اضافه کن.
    private static final Map<String, String> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("فارسی", TranslateLanguage.PERSIAN);
        LANGUAGES.put("انگلیسی", TranslateLanguage.ENGLISH);
        LANGUAGES.put("عربی", TranslateLanguage.ARABIC);
        LANGUAGES.put("ترکی", TranslateLanguage.TURKISH);
        LANGUAGES.put("فرانسوی", TranslateLanguage.FRENCH);
        LANGUAGES.put("آلمانی", TranslateLanguage.GERMAN);
        LANGUAGES.put("اسپانیایی", TranslateLanguage.SPANISH);
        LANGUAGES.put("ایتالیایی", TranslateLanguage.ITALIAN);
        LANGUAGES.put("روسی", TranslateLanguage.RUSSIAN);
        LANGUAGES.put("چینی", TranslateLanguage.CHINESE);
        LANGUAGES.put("ژاپنی", TranslateLanguage.JAPANESE);
        LANGUAGES.put("کره‌ای", TranslateLanguage.KOREAN);
        LANGUAGES.put("هندی", TranslateLanguage.HINDI);
        LANGUAGES.put("اردو", TranslateLanguage.URDU);
    }

    public static String[] languageNames() {
        return LANGUAGES.keySet().toArray(new String[0]);
    }

    public static String codeForName(String persianName) {
        return LANGUAGES.get(persianName);
    }

    /** زبان مبدا را خودش تشخیص می‌دهد و به زبان مقصد انتخابی ترجمه می‌کند. */
    public static void translateAuto(String text, String targetLanguageName, Callback cb) {
        String targetCode = codeForName(targetLanguageName);
        if (targetCode == null) { cb.onError("زبان مقصد ناشناخته است."); return; }
        if (text == null || text.trim().isEmpty()) { cb.onError("متنی برای ترجمه وارد نشده."); return; }

        LanguageIdentifier identifier = LanguageIdentification.getClient();
        identifier.identifyLanguage(text)
            .addOnSuccessListener(sourceCode -> {
                String from = ("und".equals(sourceCode) || sourceCode == null) ? null : sourceCode;
                if (from == null) {
                    cb.onError("زبان مبدا تشخیص داده نشد؛ متن طولانی‌تر امتحان کن.");
                    return;
                }
                if (from.equals(targetCode)) {
                    cb.onResult(text); // زبان مبدا و مقصد یکی است
                    return;
                }
                runTranslation(text, from, targetCode, cb);
            })
            .addOnFailureListener(e -> cb.onError("تشخیص زبان ناموفق بود: " + e.getMessage()));
    }

    private static void runTranslation(String text, String sourceCode, String targetCode, Callback cb) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetCode)
                .build();
        Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(unused -> translator.translate(text)
                .addOnSuccessListener(cb::onResult)
                .addOnFailureListener(e -> cb.onError("ترجمه ناموفق بود: " + e.getMessage())))
            .addOnFailureListener(e -> cb.onError("دانلود مدل زبان ناموفق بود (اینترنت/وای‌فای لازم است): " + e.getMessage()));
    }
}
