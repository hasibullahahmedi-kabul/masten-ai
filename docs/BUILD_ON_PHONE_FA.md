# ساخت APK بدون داشتن کامپیوتر

دو راه واقعی برای گرفتن فایل APK فقط با گوشی وجود دارد. **روش اول (GitHub Actions) را توصیه می‌کنم** — ساده‌تر، مطمئن‌تر، و کاملاً رایگان است.

---

## روش ۱ (پیشنهادی): ساخت در فضای ابری GitHub — فقط با مرورگر گوشی

نیازی به نصب چیزی روی گوشی نیست؛ کل ساخت (build) روی سرورهای گوگل/گیت‌هاب انجام می‌شود.

### مرحله ۱ — ساخت اکانت
به github.com برو (از مرورگر گوشی) و یک اکانت رایگان بساز.

### مرحله ۲ — ساخت یک Repository جدید
- دکمه‌ی «+» بالا سمت راست → «New repository»
- یک اسم بگذار (مثلا `masten-ai`)، Public یا Private فرقی نمی‌کند → «Create repository»

### مرحله ۳ — آپلود فایل‌های پروژه
- روی صفحه‌ی repository → «Add file» → «Upload files»
- فایل zip این پروژه را که از Claude گرفتی، اول باید از حالت zip خارج کنی (اکثر مرورگرها/فایل‌منیجرهای گوشی امکان Extract دارند)، بعد پوشه‌ی داخلش (`Masten_FINAL_PRODUCTION`) را از فایل‌منیجر گوشی به همین صفحه Upload بکشی یا با «choose your files» انتخاب کنی. اگر مرورگر گوشی اجازه‌ی آپلود پوشه نداد، فایل‌ها را تک‌تک با حفظ مسیر آپلود کن، یا از اپ **GitHub Mobile** یا یک اپ ترمینال با git (مثل Termux، پایین توضیح داده شده) استفاده کن.
- پایین صفحه «Commit changes» را بزن.

فایل `.github/workflows/build.yml` که همراه پروژه است، خودش به گیت‌هاب می‌گوید چطور APK بسازد — کار دیگری لازم نیست.

### مرحله ۴ — دیدن نتیجه ساخت
- برو به تب «Actions» بالای صفحه‌ی repository.
- یک اجرا با نام «Build Masten APK» می‌بینی؛ رویش بزن و صبر کن (معمولاً ۵ تا ۱۰ دقیقه طول می‌کشد).
- وقتی علامت ✅ سبز ظاهر شد، پایین همان صفحه بخش «Artifacts» را باز کن و «masten-debug-apk» را دانلود کن (یک فایل zip حاوی APK).

### مرحله ۵ — نصب روی گوشی
- فایل zip دانلود‌شده را Extract کن تا `app-debug.apk` بیرون بیاید.
- روی فایل apk بزن؛ اگر گوشی پیام «نصب از منابع ناشناس» داد، اجازه‌اش را بده (Settings › نصب اپ‌های ناشناس، فقط برای همین بار/همین فایل).
- Masten نصب می‌شود.

هر بار که کد را تغییر دادی و دوباره در گیت‌هاب Commit کردی، Action دوباره خودکار اجرا می‌شود و APK جدید می‌سازد.

---

## روش ۲: ساخت کاملاً روی خود گوشی با Termux (بدون اینترنت پیوسته، ولی سنگین‌تر)

این روش کاملاً آفلاین (بعد از دانلود اولیه) کار می‌کند ولی به حداقل ۴-۶ گیگابایت فضای خالی و گوشی نسبتاً قوی نیاز دارد؛ ممکن است کند یا شکننده باشد.

```bash
pkg update && pkg upgrade
pkg install openjdk-17 wget unzip git

# دانلود Android command-line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip -d $HOME/android-sdk/cmdline-tools
mv $HOME/android-sdk/cmdline-tools/cmdline-tools $HOME/android-sdk/cmdline-tools/latest

export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# دانلود Gradle
wget https://services.gradle.org/distributions/gradle-8.7-bin.zip
unzip gradle-8.7-bin.zip -d $HOME
export PATH=$PATH:$HOME/gradle-8.7/bin

# داخل پوشه‌ی پروژه (Masten_FINAL_PRODUCTION)
gradle assembleDebug
```
فایل خروجی در `android/app/build/outputs/apk/debug/app-debug.apk` ساخته می‌شود؛ همان‌جا رویش بزن تا نصب شود.

---

## نکته درباره‌ی قابلیت «دوبله فیلم/سریال»
این قابلیت مستقل از ساخت APK است و همچنان به یک Backend پایتون نیاز دارد که ffmpeg/Whisper/TTS رویش اجرا شود — این کار روی گوشی عملی نیست، چون منابع پردازشی زیادی می‌خواهد.
اگر فعلاً کامپیوتر نداری، بقیه‌ی اپ (چت محلی، ترجمه با ML Kit، پیامک/اشتراک‌گذاری، اعلان‌ها) کاملاً مستقل از Backend کار می‌کند؛ فقط دوبله و پاسخ هوش مصنوعی محلی (Ollama) تا وقتی Backend نداری در دسترس نیست. بعداً هر وقت به کامپیوتر یا یک سرور ابری ارزان (VPS) دسترسی داشتی، طبق `docs/DUBBING_FA.md` راه‌اندازیش کن.
