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

def nz(): return random.random()*2-1

random.seed(31)

# Cinematic station room tone: low HVAC drone, subtle electrical hum and distant metallic resonance.
def roomtone(t):
    total=12.0
    low=0.022*math.sin(2*math.pi*(31+0.6*math.sin(t*.22))*t)
    hum=0.016*math.sin(2*math.pi*60*t)+0.006*math.sin(2*math.pi*120*t)
    distant=0.009*math.sin(2*math.pi*(147+5*math.sin(t*.17))*t)
    return (low+hum+distant+nz()*0.005)*env(t,total,.9,1.2)
write_wav('fluorescent_buzz',tone(12.0,roomtone))

# CCTV signal collapse with a sharp film-style sting and low-frequency impact.
def cctv(t):
    total=2.7
    static=nz()*(0.11 if t<1.9 else 0.11*max(0,(total-t)/.8))
    sting=0.19*math.sin(2*math.pi*(1500-500*t)*t)*math.exp(-3.4*t)
    hit=0.30*math.sin(2*math.pi*45*max(0,t-.10))*math.exp(-6.3*max(0,t-.10)) if t>.10 else 0
    return static+sting+hit
write_wav('cctv_static',tone(2.7,cctv))

# Slow J-horror suspense drone from deep inside the tunnel.
def tunnel(t):
    total=11.0; rise=min(1,t/7.0)
    sub=(0.026+0.055*rise)*math.sin(2*math.pi*(29+1.6*math.sin(t*.31))*t)
    bowed=0.017*math.sin(2*math.pi*(184+10*math.sin(t*.14))*t)
    pulse=0.012*math.sin(2*math.pi*.39*t)*math.sin(2*math.pi*72*t)
    return (sub+bowed+pulse+nz()*0.007*rise)*env(t,total,1.5,1.4)
write_wav('tunnel_rumble',tone(11.0,tunnel))

# Sparse cinematic metal impacts with long ringing tails.
samples=[0.0]*int(6.0*SR)
for hit,amp,pitch in ((0.55,.40,116),(2.28,.31,91),(4.62,.46,74)):
    for i in range(int(1.15*SR)):
        t=i/SR; idx=int((hit+t)*SR)
        if idx<len(samples):
            samples[idx]+=amp*math.exp(-4.0*t)*math.sin(2*math.pi*(pitch-16*t)*t)
            samples[idx]+=amp*.28*math.exp(-7.5*t)*math.sin(2*math.pi*(pitch*2.7)*t)
write_wav('metal_knocks',samples)

# Camera relay click remains realistic, with a tiny low film hit under it.
def click(t):
    relay=0.34*math.exp(-28*t)*math.sin(2*math.pi*860*t)
    thunk=0.15*math.exp(-14*t)*math.sin(2*math.pi*170*t)
    sub=0.08*math.exp(-10*t)*math.sin(2*math.pi*52*t)
    return relay+thunk+sub
write_wav('camera_click',tone(.48,click))

# Japanese commuter-style electric horn.
def horn(t):
    total=3.8; e=env(t,total,.12,.45)
    vibr=1.0+0.003*math.sin(2*math.pi*5.2*t)
    a=0.15*math.sin(2*math.pi*370*vibr*t)
    b=0.12*math.sin(2*math.pi*466*vibr*t)
    c=0.045*math.sin(2*math.pi*740*t)
    return (a+b+c+nz()*0.018)*e
write_wav('train_horn',tone(3.8,horn))

# Long commuter-train rolling bed: electric motor, rail joints and wheel noise.
def roll(t):
    hum=0.055*math.sin(2*math.pi*(43+2*math.sin(t*.7))*t)+0.032*math.sin(2*math.pi*86*t)
    phase=t%0.46
    clack=0.19*math.exp(-37*phase)*math.sin(2*math.pi*145*phase) if phase<.12 else 0
    return (hum+nz()*0.023+clack)*env(t,12.0,.9,.9)
write_wav('train_roll',tone(12.0,roll))

# Brake squeal + pneumatic release.
def brakes(t):
    total=5.4; e=env(t,total,.18,.65)
    f=1280-135*t
    squeal=(0.095*math.sin(2*math.pi*f*t)+0.032*math.sin(2*math.pi*(f*1.7)*t))*e
    hiss=nz()*0.055*(1 if 3.35<t<5.0 else 0)
    return squeal+hiss
write_wav('train_brakes',tone(5.4,brakes))

# Realistic commuter-door warning chime.
s=tone(2.7, lambda t: 0.15*math.sin(2*math.pi*(880 if int(t*4)%2==0 else 660)*t)*env(t,2.7,.03,.15))
write_wav('door_chime',s)

# Power-down moment: electrical collapse + cinematic sub hit.
def power(t):
    buzz=0.11*math.sin(2*math.pi*60*t)*max(0,1-t/1.05)
    thump=0.44*math.exp(-7.5*max(0,t-.62))*math.sin(2*math.pi*47*max(0,t-.62)) if t>.62 else 0
    air=nz()*0.025*max(0,1-abs(t-1.0)/.65)
    return buzz+thump+air
write_wav('power_down',tone(2.1,power))

# Female-horror scream/roar texture: original synthesis, no game creature sample.
def roar(t):
    total=4.7; e=env(t,total,.14,.65)
    base=168+22*math.sin(2*math.pi*.62*t)+8*math.sin(2*math.pi*2.0*t)
    vocal=0.13*math.sin(2*math.pi*base*t)+0.07*math.sin(2*math.pi*base*2.02*t)
    rasp=nz()*0.095*(0.45+0.55*math.sin(2*math.pi*6.5*t)**2)
    sub=0.06*math.sin(2*math.pi*48*t)
    return (vocal+rasp+sub)*e
write_wav('girl_roar',tone(4.7,roar))

# Film-style tension swells and bass impacts used around reveals/blackout.
def reverse_swell(t):
    total=3.8; rise=(t/total)**2
    high=0.045*math.sin(2*math.pi*(380+240*t)*t)
    air=nz()*0.09
    return (high+air)*rise*env(t,total,.15,.08)
write_wav('horror_swell',tone(3.8,reverse_swell))

def bass_hit(t):
    return 0.48*math.exp(-4.6*t)*math.sin(2*math.pi*(43-5*t)*t)+0.08*nz()*math.exp(-8*t)
write_wav('horror_hit',tone(2.4,bass_hit))

# Very quiet breath bed for the blackout section.
def breath(t):
    total=8.0
    cycle=(math.sin(2*math.pi*.19*t)+1)/2
    airy=nz()*0.025*cycle
    low=0.012*math.sin(2*math.pi*53*t)*cycle
    return (airy+low)*env(t,total,.8,.9)
write_wav('female_breath',tone(8.0,breath))
