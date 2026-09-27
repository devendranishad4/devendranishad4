from pathlib import Path
import wave, math, random, struct, subprocess

SR=44100
out=Path('src/main/resources/assets/train31/sounds'); out.mkdir(parents=True,exist_ok=True)

def write_wav(name, samples):
    p=Path('/tmp')/(name+'.wav')
    with wave.open(str(p),'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        frames=bytearray()
        for s in samples:
            s=max(-1.0,min(1.0,s)); frames += struct.pack('<h', int(s*32767))
        w.writeframes(frames)
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(p),'-c:a','libvorbis','-q:a','5',str(out/(name+'.ogg'))],check=True)

def tone(sec, fn):
    n=int(sec*SR); return [fn(i/SR) for i in range(n)]

random.seed(31)
# fluorescent electrical buzz, low and unobtrusive
s=tone(8.0, lambda t: 0.05*math.sin(2*math.pi*60*t)+0.018*math.sin(2*math.pi*120*t)+(random.random()*2-1)*0.006)
write_wav('fluorescent_buzz',s)
# CCTV static burst
s=tone(2.2, lambda t: (random.random()*2-1)*(0.18 if t<1.7 else 0.18*(2.2-t)/0.5))
write_wav('cctv_static',s)
# distant tunnel rumble
s=tone(9.0, lambda t: (0.035+0.055*min(1,t/7))*math.sin(2*math.pi*(32+3*math.sin(t*.7))*t)+(random.random()*2-1)*0.008)
write_wav('tunnel_rumble',s)
# three restrained metal knocks
samples=[0.0]*int(4.5*SR)
for hit in (0.45,1.55,3.05):
    for i in range(int(.42*SR)):
        t=i/SR; idx=int((hit+t)*SR)
        if idx<len(samples): samples[idx]+=0.45*math.exp(-11*t)*math.sin(2*math.pi*(115-35*t)*t)
write_wav('metal_knocks',samples)
# camera relay click
s=tone(.38, lambda t: 0.38*math.exp(-28*t)*math.sin(2*math.pi*860*t)+0.14*math.exp(-16*t)*math.sin(2*math.pi*180*t))
write_wav('camera_click',s)
# deep arrival horn - original synthetic design
s=tone(4.0, lambda t: (0.11*math.sin(2*math.pi*86*t)+0.08*math.sin(2*math.pi*129*t))*min(1,t/.25)*min(1,(4-t)/.5))
write_wav('train_horn',s)
