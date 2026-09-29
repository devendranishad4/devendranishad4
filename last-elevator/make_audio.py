"""Create original horror cues for the mod; no third-party recordings."""
from pathlib import Path
import subprocess
import tempfile
import wave

import numpy as np
from scipy.signal import butter, sosfilt

ROOT = Path(__file__).resolve().parent
OUT = ROOT / 'mod/src/main/resources/assets/lastelevator/sounds'
OUT.mkdir(parents=True, exist_ok=True)
SR = 22050
rng = np.random.default_rng(34017)


def timeline(seconds):
    return np.arange(int(seconds * SR)) / SR


def noise(seconds, low=None, high=None):
    sample = rng.standard_normal(int(seconds * SR))
    if high is not None:
        sample = sosfilt(butter(3, high, btype='lowpass', fs=SR, output='sos'), sample)
    if low is not None:
        sample = sosfilt(butter(3, low, btype='highpass', fs=SR, output='sos'), sample)
    return sample / max(np.std(sample), .001)


def envelope(t, attack=.04, release=.3):
    return np.minimum(1, t / attack) * np.minimum(1, (t[-1] - t) / release)


def add_at(buffer, clip, second, amplitude=1):
    start = int(second * SR)
    end = min(len(buffer), start + len(clip))
    buffer[start:end] += clip[:end - start] * amplitude


def save(name, audio):
    audio = np.tanh(np.asarray(audio, dtype=np.float64) * 1.15) * .8
    with tempfile.NamedTemporaryFile(suffix='.wav') as tmp:
        with wave.open(tmp.name, 'wb') as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(SR)
            wav.writeframes((audio * 32767).astype('<i2').tobytes())
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', tmp.name,
                        '-c:a', 'libvorbis', '-qscale:a', '4',
                        str(OUT / (name + '.ogg'))], check=True)


t = timeline(6)
phase = 2 * np.pi * (43*t + 3*t*t/6 + .7*np.sin(t*3))
motor = (.16*np.sin(phase) + .09*np.sin(phase*2.02) +
         .06*noise(6, high=320) + .035*np.sin(2*np.pi*11*t)*noise(6, low=180, high=1100))
save('lift_motor', motor * envelope(t, .9, 1.4))

t = timeline(3)
breath = np.zeros_like(t)
for start, duration, strength in [(.1,.85,.17),(1.3,1.2,.23)]:
    u = timeline(duration)
    clip = (noise(duration, low=65, high=700)*.8 + noise(duration, low=140, high=2400)*.2)
    clip *= np.sin(np.pi * np.arange(len(u))/len(u))**1.7
    add_at(breath, clip, start, strength)
save('passenger_breath', breath)

t = timeline(1.7)
shriek = (.17*np.sin(2*np.pi*(143*t-18*t*t)) + .11*np.sin(2*np.pi*(61*t-5*t*t)) +
          .15*noise(1.7, low=260, high=4100))
save('horror_sting', shriek * np.exp(-2.6*t) * envelope(t, .013, .03))

t = timeline(10)
hum = (.055*np.sin(2*np.pi*49*t) + .027*np.sin(2*np.pi*98*t) +
       .04*noise(10, high=210) + .016*noise(10, low=450, high=1600))
save('floor_ambience', hum * envelope(t, .65, .8) * (1 + .16*np.sin(2*np.pi*.27*t)))

t = timeline(2.35)
door = (.09*np.sin(2*np.pi*(63*t+8*t*t)) + .075*noise(2.35, low=130, high=1700))
door *= envelope(t, .12, .5) * (1 + .22*np.sin(2*np.pi*17*t))
u = timeline(.23)
latch = (.33*noise(.23, low=180, high=2500) + .18*np.sin(2*np.pi*92*u))*np.exp(-21*u)
add_at(door, latch, 1.84)
save('lift_door', door)

t = timeline(2.1)
bell = (.22*np.sin(2*np.pi*920*t) + .10*np.sin(2*np.pi*1381*t) +
        .06*np.sin(2*np.pi*2280*t)) * np.exp(-2.2*t)
add_at(bell, bell.copy()*.35, .19)
save('lift_bell', bell*envelope(t, .004, .08))

steps = np.zeros(len(timeline(3.5)))
for i, start in enumerate([.13,.68,1.27,1.91,2.61]):
    u = timeline(.27)
    thud = (.21*noise(.27, high=230)+.09*noise(.27, low=250, high=1700))*np.exp(-19*u)
    add_at(steps, thud, start, .95 - .15*i)
save('distant_steps', steps)

t = timeline(4.2)
carrier = noise(4.2, low=400, high=3300)*.085
carrier *= (.24 + .76*(np.sin(2*np.pi*6.4*t)>.45))
radio = carrier + .03*np.sin(2*np.pi*1300*t)*(np.sin(2*np.pi*1.9*t)>.2)
save('broken_radio', radio*envelope(t, .08, .55))

knock = np.zeros(len(timeline(2.4)))
for start, volume in [(.19,1),(.72,.87),(1.49,.72)]:
    u = timeline(.32)
    hit = (.26*noise(.32, high=260)+.075*noise(.32, low=200, high=1200))*np.exp(-17*u)
    add_at(knock, hit, start, volume)
save('distant_knock', knock)

t = timeline(3)
fault = .05*np.sin(2*np.pi*96*t) + .025*np.sin(2*np.pi*192*t)
for start, duration in [(.3,.08),(.56,.16),(1.1,.12),(1.53,.23),(2.24,.1)]:
    u = timeline(duration)
    add_at(fault, .22*noise(duration, low=500, high=6500)*np.exp(-12*u), start)
save('electrical_fault', fault*envelope(t, .02, .4))
