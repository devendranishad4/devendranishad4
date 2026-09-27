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
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(p),'-c:a','libvorbis','-q:a','6',str(out/(name+'.ogg'))],check=True)

def tone(sec, fn):
    n=int(sec*SR); return [fn(i/SR) for i in range(n)]

def env(t,total,attack=.08,release=.25):
    return min(1,t/max(.001,attack))*min(1,max(0,total-t)/max(.001,release))

random.seed(31)

# Station fluorescent ambience
s=tone(8.0, lambda t: 0.05*math.sin(2*math.pi*60*t)+0.018*math.sin(2*math.pi*120*t)+(random.random()*2-1)*0.006)
write_wav('fluorescent_buzz',s)

# CCTV static burst
s=tone(2.2, lambda t: (random.random()*2-1)*(0.18 if t<1.7 else 0.18*(2.2-t)/0.5))
write_wav('cctv_static',s)

# Tunnel rumble
s=tone(9.0, lambda t: (0.035+0.055*min(1,t/7))*math.sin(2*math.pi*(32+3*math.sin(t*.7))*t)+(random.random()*2-1)*0.008)
write_wav('tunnel_rumble',s)

# Restrained metal knocks
samples=[0.0]*int(4.5*SR)
for hit in (0.45,1.55,3.05):
    for i in range(int(.42*SR)):
        t=i/SR; idx=int((hit+t)*SR)
        if idx<len(samples): samples[idx]+=0.45*math.exp(-11*t)*math.sin(2*math.pi*(115-35*t)*t)
write_wav('metal_knocks',samples)

# Camera relay click
s=tone(.38, lambda t: 0.38*math.exp(-28*t)*math.sin(2*math.pi*860*t)+0.14*math.exp(-16*t)*math.sin(2*math.pi*180*t))
write_wav('camera_click',s)

# Japanese-commuter-style electric horn: layered two-tone fundamental + upper harmonics + air onset.
def horn(t):
    total=3.8; e=env(t,total,.12,.45)
    vibr=1.0+0.003*math.sin(2*math.pi*5.2*t)
    a=0.15*math.sin(2*math.pi*370*vibr*t)
    b=0.12*math.sin(2*math.pi*466*vibr*t)
    c=0.045*math.sin(2*math.pi*740*t)
    breath=(random.random()*2-1)*0.018
    return (a+b+c+breath)*e
write_wav('train_horn',tone(3.8,horn))

# Rolling train: motor hum + rail joint rhythm + wheel noise.
def roll(t):
    hum=0.06*math.sin(2*math.pi*(44+2*math.sin(t*.7))*t)+0.035*math.sin(2*math.pi*88*t)
    noise=(random.random()*2-1)*0.025
    phase=t%0.46
    clack=0.20*math.exp(-38*phase)*math.sin(2*math.pi*145*phase) if phase<.12 else 0
    return (hum+noise+clack)*env(t,10.0,.8,.8)
write_wav('train_roll',tone(10.0,roll))

# Brake squeal and pneumatic release.
def brakes(t):
    total=5.0; e=env(t,total,.15,.6)
    f=1250-130*t
    squeal=(0.10*math.sin(2*math.pi*f*t)+0.035*math.sin(2*math.pi*(f*1.7)*t))*e
    hiss=(random.random()*2-1)*0.05*(1 if 3.2<t<4.7 else 0)
    return squeal+hiss
write_wav('train_brakes',tone(5.0,brakes))

# Clean commuter-door warning chime.
s=tone(2.5, lambda t: (0.16*math.sin(2*math.pi*(880 if int(t*4)%2==0 else 660)*t))*env(t,2.5,.03,.15))
write_wav('door_chime',s)

# Power failure: electrical drop and low transformer thump.
def power(t):
    buzz=0.12*math.sin(2*math.pi*60*t)*max(0,1-t/1.1)
    thump=0.42*math.exp(-8*max(0,t-.65))*math.sin(2*math.pi*58*max(0,t-.65)) if t>.65 else 0
    return buzz+thump
write_wav('power_down',tone(1.8,power))

# Original non-speech creature roar: low vocal-like harmonics + turbulent noise, no sampled media.
def roar(t):
    total=4.5; e=env(t,total,.18,.55)
    base=72+11*math.sin(2*math.pi*.65*t)+5*math.sin(2*math.pi*2.1*t)
    vocal=0.17*math.sin(2*math.pi*base*t)+0.09*math.sin(2*math.pi*base*2.03*t)+0.05*math.sin(2*math.pi*base*3.1*t)
    rasp=(random.random()*2-1)*0.11*(0.5+0.5*math.sin(2*math.pi*7*t)**2)
    return (vocal+rasp)*e
write_wav('girl_roar',tone(4.5,roar))
