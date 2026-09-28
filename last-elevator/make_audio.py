"""Synthesize original, quiet in-game ambience; no downloaded samples."""
from pathlib import Path
import subprocess,tempfile,wave
import numpy as np

out=Path('mod/src/main/resources/assets/lastelevator/sounds');out.mkdir(parents=True,exist_ok=True)
sr=22050
rng=np.random.default_rng(34017)

def save(name,data):
    data=np.asarray(data,dtype=np.float64)
    data=np.clip(data,-.98,.98)
    with tempfile.NamedTemporaryFile(suffix='.wav') as tmp:
        with wave.open(tmp.name,'wb') as w:
            w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr)
            w.writeframes((data*32767).astype('<i2').tobytes())
        subprocess.run(['ffmpeg','-v','error','-y','-i',tmp.name,'-c:a','libvorbis','-qscale:a','3',str(out/(name+'.ogg'))],check=True)

def low_noise(n,width):
    x=rng.normal(size=n)
    return np.convolve(x,np.ones(width)/width,mode='same')

t=np.arange(sr*6)/sr
env=np.minimum(1,t/1.5)*np.minimum(1,(6-t)/1.5)
motor=(.22*np.sin(2*np.pi*(54*t+2*t*t/6))+.09*np.sin(2*np.pi*112*t)+.09*low_noise(len(t),55))*env
save('lift_motor',motor)

t=np.arange(sr*3)/sr
breath=(.24*low_noise(len(t),14)+.07*np.sin(2*np.pi*80*t))
waveform=(np.sin(2*np.pi*.7*t-np.pi/2)+1)/2
save('passenger_breath',breath*waveform*np.minimum(1,t/.25)*np.minimum(1,(3-t)/.3))

t=np.arange(int(sr*1.7))/sr
sting=(.30*np.sin(2*np.pi*(140*t-24*t*t))+.18*np.sin(2*np.pi*57*t)+.14*rng.normal(size=len(t)))
save('horror_sting',sting*np.exp(-2.3*t)*np.minimum(1,t/.025))

t=np.arange(sr*10)/sr
amb=.095*low_noise(len(t),70)+.045*np.sin(2*np.pi*44*t)
amb+=.018*np.sin(2*np.pi*(70*t+3*np.sin(.33*t)))
save('floor_ambience',amb*np.minimum(1,t/.5)*np.minimum(1,(10-t)/.5))
