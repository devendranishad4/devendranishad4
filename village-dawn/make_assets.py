"""Original texture/SFX plus clearly labelled generic rehearsal speech, not an Injaa voice clone."""
import json,math,random,wave,struct,subprocess,tempfile
from pathlib import Path
from PIL import Image,ImageDraw
import nbtlib
R=Path(__file__).parent/'mod/src/main/resources'
A=R/'assets/villagedawn';(A/'sounds').mkdir(parents=True,exist_ok=True);(A/'textures/entity').mkdir(parents=True,exist_ok=True)
rng=random.Random(203)
im=Image.new('RGB',(64,64));pix=im.load()
for y in range(64):
 for x in range(64):
  base=40 if y<19 else 72;v=base+rng.randrange(-10,11);pix[x,y]=(v,v,max(0,v-3))
d=ImageDraw.Draw(im);d.rectangle((8,3,15,10),fill=(15,14,13));d.rectangle((9,6,10,7),fill=(94,100,86));d.rectangle((13,6,14,7),fill=(94,100,86));im.save(A/'textures/entity/caller.png')
(A/'models/item').mkdir(parents=True,exist_ok=True)
for name,tex in [('bell_rope','string'),('fixing_pin','iron_nugget'),('bakery_key','tripwire_hook')]:
 (A/'models/item'/f'{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'minecraft:item/{tex}'}}))
(A/'lang').mkdir(exist_ok=True);(A/'lang/en_us.json').write_text(json.dumps({'item.villagedawn.bell_rope':'Bell Rope','item.villagedawn.fixing_pin':'Fixing Pin','item.villagedawn.bakery_key':'Bakery Key','entity.villagedawn.caller':'The Caller','key.villagedawn.director':'Open Village Dawn Director','key.categories.villagedawn':'Village Dawn'}))
names=['knock','breath','roar','sting','elder','false_elder','imitation']
(A/'sounds.json').write_text(json.dumps({name:{'sounds':[{'name':f'villagedawn:{name}','stream':False}]} for name in names}))
with tempfile.TemporaryDirectory() as td:
 for name,seconds in [('knock',2.5),('breath',3.2),('roar',2.1),('sting',1.5)]:
  hz=22050;buf=[]
  for i in range(int(hz*seconds)):
   t=i/hz;noise=rng.uniform(-1,1);env=math.sin(math.pi*t/seconds)**2
   if name=='knock':
    amp=sum(math.exp(-(t-hit)*27) if t>=hit else 0 for hit in [.15,.85,1.55]);v=(noise*.55+math.sin(t*2*math.pi*90)*.45)*amp*.65
   elif name=='breath':v=noise*env*(.2+.13*math.sin(t*2*math.pi*170))
   elif name=='roar':v=env*(noise*.32+math.sin(t*2*math.pi*(55+9*math.sin(t*4)))*.48+math.sin(t*2*math.pi*112)*.18)
   else:v=env*(math.sin(t*2*math.pi*(95+50*t))*.48+noise*.25)
   buf.append(struct.pack('<h',int(max(-1,min(1,v))*23000)))
  wav=Path(td)/f'{name}.wav'
  with wave.open(str(wav),'wb') as f:f.setnchannels(1);f.setsampwidth(2);f.setframerate(hz);f.writeframes(b''.join(buf))
  subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(A/'sounds'/f'{name}.ogg')],check=True)
 lines={
 'elder':'सुबह से पहले निकल जाना। पहली घंटी के बाद कोई आवाज बुलाए, दरवाजा मत खोलना। चाहे मेरी ही आवाज हो।',
 'false_elder':'बेटा नीचे आ जाओ। तुम्हें दूसरा कमरा दिखाना है।',
 'imitation':'एक बेड मिल जाए तो सुबह निकल जाऊंगा।'}
 for name,line in lines.items():
  wav=Path(td)/f'{name}.wav';subprocess.run(['espeak-ng','-v','hi','-s','115','-p','28','-w',str(wav),line],check=True)
  filt='highpass=f=120,lowpass=f=2800,volume=0.8' if name=='elder' else 'asetrate=18000,aresample=22050,aecho=0.65:0.6:70:0.18,volume=0.75'
  subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-af',filt,'-c:a','libvorbis','-q:a','4',str(A/'sounds'/f'{name}.ogg')],check=True)
(R/'data/villagedawn/structures').mkdir(parents=True,exist_ok=True)
template=nbtlib.Compound({'DataVersion':nbtlib.Int(3465),'size':nbtlib.List[nbtlib.Int]([10,6,10]),'palette':nbtlib.List[nbtlib.Compound]([nbtlib.Compound({'Name':nbtlib.String('minecraft:air')})]),'blocks':nbtlib.List[nbtlib.Compound]([]),'entities':nbtlib.List[nbtlib.Compound]([])})
nbtlib.File(template,gzipped=True).save(R/'data/villagedawn/structures/empty.nbt')
print('Generated original SFX/model texture and generic Hindi rehearsal speech.')
