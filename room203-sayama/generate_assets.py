"""Generate original sound design and skin; no borrowed audio or map redistribution."""
from pathlib import Path
import json, subprocess, wave, tempfile
import numpy as np
from PIL import Image, ImageDraw
ROOT=Path(__file__).parent/'mod/src/main/resources'
A=ROOT/'assets/room203';(A/'sounds').mkdir(parents=True,exist_ok=True)
rng=np.random.default_rng(203); sr=44100

def fade(x,seconds=.2):
 n=min(int(sr*seconds),len(x)//2); x[:n]*=np.linspace(0,1,n);x[-n:]*=np.linspace(1,0,n);return x

def room(t):
 # Layered original narrowband room tone: a ventilation hum with irregular gusts.
 white=rng.normal(0,1,len(t)); smooth=np.convolve(white,np.ones(91)/91,mode='same')
 return .06*np.sin(2*np.pi*49*t)+.04*np.sin(2*np.pi*98*t)+.19*smooth*(.6+.4*np.sin(2*np.pi*.13*t)**2)

def impact(t,when,level=1,freq=130):
 q=t-when; return np.where(q>=0,level*np.exp(-np.maximum(q,0)*22)*(np.sin(2*np.pi*freq*q)+.4*np.sin(2*np.pi*freq*2.08*q)),0)

def create(name,duration):
 t=np.arange(int(sr*duration))/sr
 if name=='rain':
  x=.10*rng.normal(0,1,len(t))+.05*np.sin(2*np.pi*41*t)
  for when in rng.uniform(0,duration,160):x+=impact(t,when,.10,1200)
 elif name=='hall':x=room(t)
 elif name=='knock':
  x=np.zeros_like(t)
  for when in [.25,1.02,1.80]:x+=impact(t,when,.65,95)+impact(t,when+.035,.16,210)
 elif name=='steps':
  x=np.zeros_like(t)
  for when in [.25,.9,1.55,2.2,2.85,3.5]:x+=impact(t,when,.36,70)+impact(t,when+.07,.15,230)
 elif name=='breath':
  noise=rng.normal(0,1,len(t));noise=np.convolve(noise,np.ones(9)/9,mode='same')
  x=.32*noise*np.sin(np.pi*t/duration)**2+.045*np.sin(2*np.pi*75*t)*np.sin(np.pi*t/duration)
 elif name=='fault':
  x=.14*np.sin(2*np.pi*100*t)*np.exp(-t*8)
  for when in [.08,.22,.48]:x+=impact(t,when,.4,700)
 elif name=='sting':
  phase=2*np.pi*(65*t+95*t*t);x=(.14*np.sin(phase)+.13*np.sin(phase*1.073)+.05*rng.normal(0,1,len(t)))*np.exp(-t*1.6)
 elif name=='pursuit':
  x=room(t)*.6+.09*np.sin(2*np.pi*38*t)
  for when in np.arange(.25,duration,.75):x+=impact(t,float(when),.32,55)+impact(t,float(when)+.16,.16,63)
 x=fade(x);x=np.clip(x,-.93,.93)
 with tempfile.TemporaryDirectory() as d:
  f=Path(d)/'clip.wav'
  with wave.open(str(f),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((x*32767).astype('<i2').tobytes())
  subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(f),'-c:a','libvorbis','-q:a','6',str(A/'sounds'/f'{name}.ogg')],check=True)
 return {'sounds':[{'name':f'room203:{name}','stream':duration>10}], 'subtitle':f'subtitles.room203.{name}'}

sounds={name:create(name,dur) for name,dur in {'rain':30,'hall':30,'knock':3.1,'steps':4.2,'breath':1.8,'fault':1.1,'sting':2,'pursuit':32}.items()}
(A/'sounds.json').write_text(json.dumps(sounds,indent=2))
lang={'entity.room203.visitor':'The Neighbour',**{f'subtitles.room203.{x}':s for x,s in {'rain':'Rain on concrete','hall':'Empty building hum','knock':'Three knocks','steps':'Footsteps upstairs','breath':'Someone breathing','fault':'Light fails','sting':'A presence','pursuit':'Footsteps behind you'}.items()}}
(A/'lang').mkdir(exist_ok=True);(A/'lang/en_us.json').write_text(json.dumps(lang,indent=2))
# 64x64 Minecraft humanoid UV layout, original charcoal coat and pale recessed face.
im=Image.new('RGBA',(64,64),(0,0,0,0));d=ImageDraw.Draw(im)
for box in [(0,0,31,15),(0,16,55,31),(16,48,47,63)]:d.rectangle(box,fill=(18,20,23,255))
for x in range(64):
 for y in range(64):
  if im.getpixel((x,y))[3]:
   n=int(rng.integers(-4,5));im.putpixel((x,y),(max(0,18+n),max(0,20+n),max(0,23+n),255))
d.rectangle((8,8,15,15),fill=(148,146,132,255));d.rectangle((9,10,10,11),fill=(8,8,9,255));d.rectangle((13,10,14,11),fill=(8,8,9,255));d.line((11,13,13,13),fill=(38,30,32,255))
d.line((24,20,24,31),fill=(58,54,49,255));d.rectangle((20,29,27,30),fill=(29,30,32,255))
(A/'textures/entity').mkdir(parents=True,exist_ok=True);im.save(A/'textures/entity/visitor.png')
print('Generated 8 original OGG clips and the Visitor texture')
