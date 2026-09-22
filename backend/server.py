from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.responses import FileResponse
from pydantic import BaseModel
import sqlite3, os, json, re, csv, threading, uuid
from pathlib import Path
import urllib.request

APP=FastAPI(title='Masten AI',version='1.0')
DB='masten.db'; OLLAMA=os.getenv('OLLAMA_URL','http://127.0.0.1:11434'); MODEL=os.getenv('OLLAMA_MODEL','llama3')
DUB_JOBS_DIR=Path('dub_jobs'); DUB_JOBS_DIR.mkdir(exist_ok=True)
DUB_JOBS={}  # job_id -> {'status': queued|processing|done|error, 'result': path|None, 'error': str|None}

class Query(BaseModel):
    query:str; confirm:bool=False

def db():
    c=sqlite3.connect(DB); c.execute('CREATE TABLE IF NOT EXISTS memory(id INTEGER PRIMARY KEY, role TEXT, text TEXT, ts DATETIME DEFAULT CURRENT_TIMESTAMP)'); c.commit(); return c

def remember(role,text):
    c=db();c.execute('INSERT INTO memory(role,text) VALUES(?,?)',(role,text));c.commit();c.close()

def ollama(prompt):
    data=json.dumps({'model':MODEL,'prompt':prompt,'stream':False}).encode()
    req=urllib.request.Request(OLLAMA+'/api/generate',data=data,headers={'Content-Type':'application/json'})
    try:
        with urllib.request.urlopen(req,timeout=90) as r:return json.loads(r.read()).get('response','')
    except Exception as e:return 'مدل محلی در دسترس نیست. Ollama و مدل انتخاب‌شده را فعال کن.'

def intent(q):
    x=q.lower()
    if any(k in x for k in ['تماس','زنگ بزن','call']):return 'call'
    if any(k in x for k in ['پیام بفرست','پیام بده','send message']):return 'message'
    if any(k in x for k in ['اینستاگرام','instagram','فیسبوک','facebook','پست کن']):return 'social'
    if any(k in x for k in ['فیلم','ویدیو','آهنگ','موسیقی','پخش کن']):return 'media'
    if any(k in x for k in ['csv','excel','داده','data science','تحلیل داده']):return 'data'
    if any(k in x for k in ['کد','برنامه','python','flutter','java','javascript','sql','کاتلین']):return 'code'
    return 'chat'

@APP.get('/health')
def health(): return {'ok':True,'ollama':OLLAMA,'model':MODEL}

@APP.post('/process')
def process(q:Query):
    it=intent(q.query); remember('user',q.query)
    if it in {'call','message','social'} and not q.confirm:
        return {'status':'confirmation_required','intent':it,'message':'این عملیات بیرونی/حساس است. ابتدا تأیید صریح کاربر لازم است.','query':q.query}
    context='\n'.join([f"{r}: {t}" for r,t in db().execute('SELECT role,text FROM memory ORDER BY id DESC LIMIT 12').fetchall()][::-1])
    prompt=f'''تو Masten هستی؛ دستیار فارسی‌زبان و عملی. پاسخ دقیق، کوتاه و مفید بده.\nحافظه اخیر:\n{context}\nدرخواست کاربر:\n{q.query}'''
    ans=ollama(prompt);remember('assistant',ans)
    return {'status':'ok','intent':it,'response':ans}

@APP.post('/analyze')
async def analyze(file:UploadFile=File(...)):
    raw=await file.read(); name=file.filename or 'file'
    if len(raw)>10_000_000: raise HTTPException(413,'file too large')
    ext=Path(name).suffix.lower()
    if ext in {'.txt','.py','.js','.dart','.java','.kt','.json','.md','.csv'}:
        text=raw.decode('utf-8','replace')
        if ext=='.csv':
            rows=list(csv.reader(text.splitlines()))
            info=f'CSV: {len(rows)} rows, {len(rows[0]) if rows else 0} columns'
        else: info=f'{ext} text file, {len(text)} characters'
        return {'name':name,'info':info,'preview':text[:12000]}
    return {'name':name,'info':'نوع فایل برای تحلیل متنی مستقیم پشتیبانی نمی‌شود.','bytes':len(raw)}


# ---- دوبله فیلم/سریال ----
# پردازش سنگین است، در پس‌زمینه با یک ترد جدا اجرا می‌شود تا درخواست HTTP بلاک نشود.
# جریان کار: آپلود ویدیو -> /dub (شروع کار) -> /dub/status/{job_id} (پایش) -> /dub/result/{job_id} (دانلود)

@APP.post('/dub')
async def start_dub(file:UploadFile=File(...), target_lang:str=Form('fa')):
    job_id=str(uuid.uuid4())
    workdir=DUB_JOBS_DIR/job_id
    workdir.mkdir(parents=True, exist_ok=True)
    video_path=workdir/(file.filename or 'input.mp4')
    raw=await file.read()
    with open(video_path,'wb') as f: f.write(raw)
    DUB_JOBS[job_id]={'status':'queued','result':None,'error':None}

    def run():
        try:
            DUB_JOBS[job_id]['status']='processing'
            from dubbing import dub_video
            out=dub_video(str(video_path), target_lang, str(workdir))
            DUB_JOBS[job_id]['result']=out
            DUB_JOBS[job_id]['status']='done'
        except Exception as e:
            DUB_JOBS[job_id]['status']='error'
            DUB_JOBS[job_id]['error']=str(e)

    threading.Thread(target=run, daemon=True).start()
    return {'job_id':job_id,'status':'queued'}

@APP.get('/dub/status/{job_id}')
def dub_status(job_id:str):
    job=DUB_JOBS.get(job_id)
    if not job: raise HTTPException(404,'job not found')
    return {'job_id':job_id,'status':job['status'],'error':job['error']}

@APP.get('/dub/result/{job_id}')
def dub_result(job_id:str):
    job=DUB_JOBS.get(job_id)
    if not job or job['status']!='done' or not job['result']:
        raise HTTPException(404,'result not ready')
    return FileResponse(job['result'], filename='dubbed.mp4', media_type='video/mp4')
