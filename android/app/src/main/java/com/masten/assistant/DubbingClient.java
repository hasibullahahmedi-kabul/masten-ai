package com.masten.assistant;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

/**
 * آپلود ویدیو به Backend برای دوبله (STT -> ترجمه -> TTS -> جایگزینی صدا)، پیگیری وضعیت، و دانلود نتیجه.
 * پردازش سنگین روی Backend (کامپیوتر) انجام می‌شود، نه روی گوشی.
 * فقط از محتوایی استفاده کن که مجاز به ویرایش/دوبله آن هستی؛ دوبله و بازنشر آثار دارای کپی‌رایت بدون مجوز، نقض حق نشر است.
 */
public class DubbingClient {

    public interface Listener {
        void onProgress(String message);
        void onDone(File outputFile);
        void onError(String message);
    }

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build();

    private final Handler main = new Handler(Looper.getMainLooper());
    private File outputDir;

    public void dub(Context ctx, String backendBaseUrl, Uri videoUri, String targetLangCode, Listener listener) {
        outputDir = new File(ctx.getExternalFilesDir(null), "Masten_Dubbed");
        post(() -> listener.onProgress("در حال آماده‌سازی فایل..."));
        new Thread(() -> {
            try {
                File temp = copyToTemp(ctx, videoUri);
                upload(backendBaseUrl, temp, targetLangCode, listener);
            } catch (Exception e) {
                post(() -> listener.onError("خطا در آماده‌سازی فایل: " + e.getMessage()));
            }
        }).start();
    }

    private File copyToTemp(Context ctx, Uri uri) throws IOException {
        File temp = new File(ctx.getCacheDir(), "masten_dub_input.mp4");
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temp)) {
            byte[] buf = new byte[8192];
            int n;
            while (in != null && (n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        return temp;
    }

    private void upload(String base, File videoFile, String targetLang, Listener listener) {
        RequestBody fileBody = RequestBody.create(videoFile, MediaType.parse("video/*"));
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", videoFile.getName(), fileBody)
                .addFormDataPart("target_lang", targetLang)
                .build();
        Request req = new Request.Builder().url(base + "/dub").post(body).build();
        post(() -> listener.onProgress("در حال آپلود ویدیو به Backend..."));
        try (Response resp = HTTP.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                post(() -> listener.onError("آپلود ناموفق بود (کد " + resp.code() + "). آدرس Backend و اتصال شبکه را بررسی کن."));
                return;
            }
            JSONObject json = new JSONObject(resp.body().string());
            String jobId = json.getString("job_id");
            poll(base, jobId, listener);
        } catch (Exception e) {
            post(() -> listener.onError("خطا در آپلود: " + e.getMessage()));
        }
    }

    private void poll(String base, String jobId, Listener listener) {
        try {
            while (true) {
                Thread.sleep(3000);
                Request req = new Request.Builder().url(base + "/dub/status/" + jobId).build();
                try (Response resp = HTTP.newCall(req).execute()) {
                    if (!resp.isSuccessful() || resp.body() == null) continue;
                    JSONObject json = new JSONObject(resp.body().string());
                    String status = json.optString("status");
                    if ("processing".equals(status) || "queued".equals(status)) {
                        post(() -> listener.onProgress("در حال دوبله روی Backend... (" + status + ")\nاین کار ممکن است چند دقیقه طول بکشد."));
                    } else if ("done".equals(status)) {
                        download(base, jobId, listener);
                        return;
                    } else if ("error".equals(status)) {
                        String err = json.optString("error", "خطای نامشخص");
                        post(() -> listener.onError("خطا در دوبله: " + err));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            post(() -> listener.onError("خطا در پیگیری وضعیت: " + e.getMessage()));
        }
    }

    private void download(String base, String jobId, Listener listener) {
        post(() -> listener.onProgress("دانلود ویدیوی دوبله‌شده..."));
        Request req = new Request.Builder().url(base + "/dub/result/" + jobId).build();
        try (Response resp = HTTP.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                post(() -> listener.onError("دانلود نتیجه ناموفق بود."));
                return;
            }
            byte[] bytes = resp.body().bytes();
            if (!outputDir.exists()) outputDir.mkdirs();
            File out = new File(outputDir, "dubbed_" + jobId + ".mp4");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(bytes);
            }
            post(() -> listener.onDone(out));
        } catch (Exception e) {
            post(() -> listener.onError("خطا در دانلود/ذخیره: " + e.getMessage()));
        }
    }

    private void post(Runnable r) { main.post(r); }
}
