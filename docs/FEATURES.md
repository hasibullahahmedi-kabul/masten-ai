# Feature matrix

| Feature | Implementation |
|---|---|
| Persian voice input | Android SpeechRecognizer |
| Persian TTS | Android TextToSpeech |
| Boot startup | BootReceiver |
| Persistent service | Foreground Service |
| Notifications | NotificationListenerService |
| Apps | PackageManager launch intents |
| Calls | ACTION_DIAL (safe confirmation boundary) |
| Media | ACTION_VIEW media intent |
| Memory | SQLite backend |
| Local AI | Ollama connector |
| Coding | Local AI prompt routing |
| Data analysis | CSV/text file endpoint |
| Social networks | Intent/API integration boundary; service-specific APIs required |
| SMS | ACTION_SENDTO با متن آماده؛ ارسال نهایی با تأیید کاربر در برنامه پیامک |
| ارسال/اشتراک‌گذاری پیام | Android Share Sheet (ACTION_SEND)؛ کاربر برنامه مقصد (واتساپ/تلگرام/ایمیل/...) و ارسال را خودش تأیید می‌کند |
| ترجمه چندزبانه | ML Kit Translate + Language Identification، روی خود دستگاه، بعد از اولین دانلود مدل هر زبان به‌صورت آفلاین |
| فیلتر حریم‌خصوصی اعلان‌ها | اعلان‌های حاوی کد تأیید/رمز/OTP با صدای بلند خوانده نمی‌شوند |
| دوبله فیلم/سریال | Backend: faster-whisper (STT) + Argos Translate (ترجمه آفلاین) + pyttsx3 (TTS) + ffmpeg (جایگزینی صدا)؛ اپ فقط آپلود/پایش/دانلود می‌کند |
