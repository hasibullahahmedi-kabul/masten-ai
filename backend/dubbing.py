"""
دوبله خودکار فیلم/سریال.
مراحل: استخراج صدا -> تشخیص گفتار (Whisper) -> ترجمه (Argos Translate، آفلاین) -> تبدیل متن به گفتار -> جایگزینی صدای ویدیو.

نکات مهم:
- این پردازش سنگین است (CPU/RAM بالا) و باید روی کامپیوتر اجرا شود، نه گوشی.
- ffmpeg و ffprobe باید نصب و در PATH سیستم باشند.
- کیفیت صداپیشگی از TTS آفلاین سیستم می‌آید؛ طبیعی‌بودن صدا و هماهنگی دقیق لب با تصویر تضمین نمی‌شود،
  فقط تلاش می‌شود طول هر جمله با کشیدن/فشردن صدا به زمان جمله اصلی نزدیک شود.
- فقط از ویدیوهایی استفاده کن که اجازه ویرایش/دوبله آن‌ها را داری (محتوای شخصی یا دارای مجوز).
  دوبله و بازنشر آثار دارای کپی‌رایت بدون اجازه صاحب اثر، نقض قانون کپی‌رایت است.
"""
import os
import subprocess

_WHISPER_MODEL = None  # فقط یک‌بار بارگذاری می‌شود


def _run_ffmpeg(args):
    subprocess.run(['ffmpeg', '-y', *args], check=True, capture_output=True)


def _duration_seconds(path: str) -> float:
    r = subprocess.run(
        ['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', path],
        capture_output=True, text=True, check=True,
    )
    return float(r.stdout.strip())


def extract_audio(video_path: str) -> str:
    wav_path = video_path + '.wav'
    _run_ffmpeg(['-i', video_path, '-ac', '1', '-ar', '16000', wav_path])
    return wav_path


def transcribe(audio_path: str):
    """گفتار را به بخش‌های زمان‌بندی‌شده تبدیل می‌کند: [{start,end,text,lang}, ...]"""
    global _WHISPER_MODEL
    from faster_whisper import WhisperModel
    if _WHISPER_MODEL is None:
        _WHISPER_MODEL = WhisperModel('small', compute_type='int8')
    segments, info = _WHISPER_MODEL.transcribe(audio_path, vad_filter=True)
    return [
        {'start': s.start, 'end': s.end, 'text': s.text.strip(), 'lang': info.language}
        for s in segments if s.text.strip()
    ]


def translate_segments(segments, source_lang: str, target_lang: str):
    """ترجمه آفلاین با Argos Translate. بسته زبانی مبدا/مقصد باید از قبل نصب شده باشد (بخش نصب در INSTALL_FA را ببین)."""
    if source_lang == target_lang:
        return [{**seg, 'translated': seg['text']} for seg in segments]
    import argostranslate.translate as at
    installed = at.get_installed_languages()
    from_lang = next((l for l in installed if l.code == source_lang), None)
    to_lang = next((l for l in installed if l.code == target_lang), None)
    if not from_lang or not to_lang:
        raise RuntimeError(
            f"بسته زبان آفلاین برای ترجمه {source_lang} -> {target_lang} نصب نیست. "
            f"راهنمای نصب را در docs/DUBBING_FA.md ببین."
        )
    translator = from_lang.get_translation(to_lang)
    return [{**seg, 'translated': translator.translate(seg['text'])} for seg in segments]


def _synthesize_segment(text: str, out_path: str):
    import pyttsx3
    engine = pyttsx3.init()
    engine.save_to_file(text, out_path)
    engine.runAndWait()
    engine.stop()


def build_dubbed_audio(segments_translated, total_duration: float, workdir: str) -> str:
    """برای هر جمله صدا می‌سازد، سرعتش را طوری تنظیم می‌کند که در بازه زمانی جمله اصلی جا شود،
    و همه را روی یک تراک صدا، هرکدام سر جای زمانی درستش، ترکیب می‌کند."""
    os.makedirs(workdir, exist_ok=True)
    silence = os.path.join(workdir, 'base_silence.wav')
    _run_ffmpeg(['-f', 'lavfi', '-i', f'anullsrc=r=22050:cl=mono', '-t', str(max(total_duration, 0.1)), silence])

    fitted_parts = []
    for idx, seg in enumerate(segments_translated, start=1):
        raw = os.path.join(workdir, f'seg_{idx}_raw.wav')
        _synthesize_segment(seg['translated'], raw)
        target_len = max(seg['end'] - seg['start'], 0.3)
        try:
            actual_len = _duration_seconds(raw)
        except Exception:
            actual_len = target_len
        tempo = actual_len / target_len if target_len > 0 else 1.0
        tempo = max(0.5, min(2.0, tempo))  # محدوده مجاز فیلتر atempo
        fitted = os.path.join(workdir, f'seg_{idx}_fit.wav')
        _run_ffmpeg(['-i', raw, '-filter:a', f'atempo={tempo:.3f}', fitted])
        fitted_parts.append((seg['start'], fitted))

    if not fitted_parts:
        return silence

    inputs = ['-i', silence]
    filter_chains = []
    mix_labels = '[0:a]'
    for i, (start, path) in enumerate(fitted_parts, start=1):
        inputs += ['-i', path]
        delay_ms = int(max(start, 0) * 1000)
        filter_chains.append(f'[{i}:a]adelay={delay_ms}|{delay_ms}[a{i}]')
        mix_labels += f'[a{i}]'
    filter_complex = ';'.join(filter_chains) + f';{mix_labels}amix=inputs={len(fitted_parts) + 1}:normalize=0[out]'

    out_path = os.path.join(workdir, 'dubbed_audio.wav')
    _run_ffmpeg([*inputs, '-filter_complex', filter_complex, '-map', '[out]', out_path])
    return out_path


def mux(video_path: str, dubbed_audio_path: str, out_path: str):
    _run_ffmpeg([
        '-i', video_path, '-i', dubbed_audio_path,
        '-map', '0:v:0', '-map', '1:a:0',
        '-c:v', 'copy', '-c:a', 'aac', '-shortest', out_path,
    ])


def dub_video(video_path: str, target_lang: str, workdir: str) -> str:
    """پایپ‌لاین کامل دوبله؛ مسیر فایل ویدیوی دوبله‌شده را برمی‌گرداند."""
    audio = extract_audio(video_path)
    segments = transcribe(audio)
    if not segments:
        raise RuntimeError('گفتاری در ویدیو تشخیص داده نشد.')
    source_lang = segments[0]['lang'] or 'en'
    translated = translate_segments(segments, source_lang, target_lang)
    total_duration = _duration_seconds(video_path)
    dubbed_audio = build_dubbed_audio(translated, total_duration, os.path.join(workdir, 'segments'))
    out_path = os.path.join(workdir, 'dubbed_output.mp4')
    mux(video_path, dubbed_audio, out_path)
    return out_path
