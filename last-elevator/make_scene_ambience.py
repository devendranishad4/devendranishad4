"""Original stereo atmospheres for the seven story spaces; deterministic synthesis."""
from pathlib import Path
import subprocess
import tempfile
import wave

import numpy as np
from scipy.signal import butter, sosfilt

OUT=Path(__file__).resolve().parent/'mod/src/main/resources/assets/lastelevator/sounds'
OUT.mkdir(parents=True,exist_ok=True)
SR=44100
rng=np.random.default_rng(421771)

def time(n):return np.arange(round(n*SR),dtype=np.float32)/SR
def noise(n,low=None,high=None):
    a=rng.standard_normal(round(n*SR)).astype(np.float32)
    if low:a=sosfilt(butter(2,low,'highpass',fs=SR,output='sos'),a).astype(np.float32)
    if high:a=sosfilt(butter(2,high,'lowpass',fs=SR,output='sos'),a).astype(np.float32)
    return a/(np.std(a)+1e-6)
def edge(t):return np.minimum(1,t/1.5)*np.minimum(1,(t[-1]-t)/2.5)
def place(a,clip,start,scale=1):
    k=round(start*SR);stop=min(len(a),k+len(clip));a[k:stop]+=clip[:stop-k]*scale
def pulse(a,starts,freq=83,decay=7,strength=.06):
    for start in starts:
        u=time(.9);place(a,np.sin(2*np.pi*freq*u)*np.exp(-decay*u),start,strength)
def stereo(name,a,b):
    pair=np.column_stack((np.tanh(a*1.25),np.tanh(b*1.25))).astype(np.float32)*.78
    with tempfile.NamedTemporaryFile(suffix='.wav') as tmp:
        with wave.open(tmp.name,'wb') as wav:
            wav.setnchannels(2);wav.setsampwidth(2);wav.setframerate(SR)
            wav.writeframes((pair*32767).astype('<i2').tobytes())
        subprocess.run(['ffmpeg','-v','error','-y','-i',tmp.name,'-c:a','libvorbis',
                        '-qscale:a','4',str(OUT/(name+'.ogg'))],check=True)
    print(name,(OUT/(name+'.ogg')).stat().st_size)

def base(n,kind):
    t=time(n);left=np.zeros_like(t);right=np.zeros_like(t)
    slow=np.sin(2*np.pi*(.08 if kind=='wide' else .13)*t)
    for target,offset in ((left,0),(right,.37)):
        target+=.026*noise(n,high=250)*(1+.2*slow)
        target+=.013*noise(n,low=200,high=2200)
        target+=.018*np.sin(2*np.pi*(48.7*t+offset))
    return t,left,right

# Rain against the glazed lobby, a far street and an elevator-machine hum.
n=42;t,l,r=base(n,'wide')
for a in (l,r):
    a+=.052*noise(n,low=850,high=9500)*(1+.1*np.sin(2*np.pi*.31*t))
    a+=.023*noise(n,high=85)*(1+.4*np.sin(2*np.pi*.06*t))
for s in (7.8,18.5,32.1):
    u=time(5.2);swoosh=noise(5.2,high=400)*np.sin(np.pi*u/5.2)**2
    place(l,swoosh,s,.04);place(r,swoosh,s+.25,.045)
stereo('tokyo_rain_lobby',l*edge(t),r*edge(t))

# Fluorescent ballast, printer rollers and occasional distant office creaks.
n=39;t,l,r=base(n,'room')
for a in (l,r):
    a+=.023*np.sin(2*np.pi*101.4*t)*(1+.14*np.sin(2*np.pi*.7*t))
    a+=.014*noise(n,low=450,high=3300)*(1+.2*np.sin(2*np.pi*.12*t))
for s in (4.6,10.8,24.7,33.4):
    u=time(1.3);paper=noise(1.3,low=500,high=6000)*np.exp(-3*u)
    place(l,paper,s,.044);place(r,paper,s+.18,.029)
pulse(l,(15.2,29.6),62);pulse(r,(15.35,29.8),62)
stereo('office_after_hours',l*edge(t),r*edge(t))

# Vacant hotel air vents, rolling service trolley and remote metallic chimes.
n=44;t,l,r=base(n,'wide')
for a in (l,r):a+=.027*noise(n,low=160,high=1400)*(1+.25*np.sin(2*np.pi*.18*t))
for s in (9.3,27.1):
    u=time(6.5);roll=noise(6.5,low=55,high=340)*(np.sin(np.pi*u/6.5)**2)
    roll*=.6+.4*np.sin(2*np.pi*8*u)**2
    place(l,roll,s,.047);place(r,roll,s+.48,.035)
for s in (17.4,36.5):
    u=time(4);chime=(np.sin(2*np.pi*731*u)+.34*np.sin(2*np.pi*1099*u))*np.exp(-1.4*u)
    place(l,chime,s,.019);place(r,chime,s+.15,.024)
stereo('hotel_thirteen_hall',l*edge(t),r*edge(t))

# Maintenance transformers, pipe knocks, electrical bursts and cable strain.
n=40;t,l,r=base(n,'room')
for a in (l,r):
    a+=.036*np.sin(2*np.pi*59.9*t)*(1+.17*np.sin(2*np.pi*.3*t))
    a+=.023*noise(n,low=180,high=4200)
for s in (5.8,13.2,19.4,29.9,35.2):
    u=time(.55);arc=noise(.55,low=700,high=12000)*np.exp(-9*u)
    place(l,arc,s,.075);place(r,arc,s+.11,.052)
pulse(l,(11.3,24.8,32.2),72,5,.11)
pulse(r,(11.5,25.0,32.4),72,5,.08)
stereo('maintenance_power_room',l*edge(t),r*edge(t))

# Repeating stairwell: draft, metal railing resonance and sparse distant steps.
n=38;t,l,r=base(n,'wide')
for a in (l,r):
    a+=.023*np.sin(2*np.pi*84*t+1.7*np.sin(2*np.pi*.07*t))
    a+=.025*noise(n,high=420)
for s in (6.2,6.9,15.4,16.1,26.7,27.4,33.8):
    u=time(.7);step=noise(.7,high=350)*np.exp(-9*u)
    place(l,step,s,.06);place(r,step,s+.13,.04)
stereo('stairwell_repeating',l*edge(t),r*edge(t))

# Floor zero: near silence punctuated by a sub-bass pulse and thin high whine.
n=41;t,l,r=base(n,'room')
for a in (l,r):
    a*=.45
    a+=.02*np.sin(2*np.pi*36.4*t)*(1+.45*np.sin(2*np.pi*.11*t))
    a+=.007*np.sin(2*np.pi*1843*t)*(1+.25*np.sin(2*np.pi*.16*t))
pulse(l,(8,18.4,30.2,37),48,2,.09)
pulse(r,(8.15,18.55,30.35,37.15),48,2,.08)
stereo('floor_zero_void',l*edge(t),r*edge(t))

# Pursuit: accelerating double heartbeat and irregular metal impacts.
n=36;t,l,r=base(n,'wide')
for a in (l,r):
    a+=.04*noise(n,low=60,high=280)*(1+.35*np.sin(2*np.pi*.3*t))
    a+=.018*np.sin(2*np.pi*(51*t+2*np.sin(2*np.pi*.09*t)))
s=1.2
while s<n-1:
    pulse(l,(s,s+.27),55,11,.13);pulse(r,(s+.06,s+.33),55,11,.12)
    s+=max(.72,1.18-.012*s)
for s in (7.9,14.5,22.1,29.3):
    u=time(1.9);hit=noise(1.9,low=80,high=2500)*np.exp(-3*u)
    place(l,hit,s,.075);place(r,hit,s+.17,.048)
stereo('passenger_pursuit',l*edge(t),r*edge(t))
