#!/usr/bin/env python3
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

BUNDLE_URL = "https://drive.google.com/drive/folders/1RXNykUKtSTMwirqxyIWxNneDqrwghHUj?usp=sharing"
TMP = Path("/tmp/train31_human_voices")
TMP.mkdir(parents=True, exist_ok=True)
OUT = Path("src/main/resources/assets/train31/sounds")
OUT.mkdir(parents=True, exist_ok=True)
PREVIEW = Path("build/preview")
PREVIEW.mkdir(parents=True, exist_ok=True)

# We deliberately use existing human performances only. We do not clone the artist's voice
# or synthesize new words in her identity. Each event is matched to a real recorded phrase.
TARGETS = {
    "whisper_injaa": [
        ("been waiting for you",), ("waiting for you",), ("hello",)
    ],
    "whisper_can_see": [
        ("can you see me",), ("can see you",), ("see you",), ("i know you are here",)
    ],
    "whisper_behind": [
        ("dont turn around",), ("don't turn around",), ("behind you",), ("beware",)
    ],
    "whisper_here": [
        ("shouldnt be here",), ("shouldn't be here",), ("should not be here",), ("i know you are here",)
    ],
    "whisper_why_here": [
        ("why did you come",), ("why are you here",), ("come to me",), ("follow me",)
    ],
    "whisper_coming": [
        ("its coming",), ("it's coming",), ("coming",), ("follow me",)
    ],
    "whisper_dont_board": [
        ("dont get on",), ("don't get on",), ("dont board",), ("don't board",), ("beware",)
    ],
    "whisper_see_you": [
        ("i can see you",), ("can see you",), ("i know you are here",)
    ],
    "whisper_cant_leave": [
        ("cant leave",), ("can't leave",), ("cannot leave",), ("waiting for you",)
    ],
    "whisper_run": [
        ("run",), ("get out",), ("leave",)
    ],
    "whisper_found_you": [
        ("i found you",), ("found you",), ("i know you are here",), ("waiting for you",)
    ],
    "whisper_should_listen": [
        ("should have listened",), ("you should listen",), ("listen",), ("beware",)
    ],
}

AUDIO_EXTS = {".wav", ".mp3", ".ogg", ".flac", ".m4a", ".aac"}
VOICE_HINTS = ("female", "woman", "women", "girl", "ghost", "witch", "vampire")
BAD_HINTS = ("male", "zombie", "creature", "monster")


def run(cmd, check=True, capture=False):
    print("+", " ".join(map(str, cmd)), flush=True)
    return subprocess.run(cmd, check=check, text=True, capture_output=capture)


def norm(s: str) -> str:
    s = s.lower().replace("’", "'")
    s = re.sub(r"[_\-]+", " ", s)
    s = re.sub(r"[^a-z0-9' ]+", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


def score_name(name: str, phrases) -> int:
    n = norm(name)
    best = -10_000
    for (phrase,) in phrases:
        p = norm(phrase)
        if not p:
            continue
        if p in n:
            score = 100 + len(p)
        else:
            words = [w for w in p.split() if len(w) > 1]
            hits = sum(1 for w in words if w in n)
            if hits != len(words):
                continue
            score = 40 + hits * 6
        if "processed" in n or "fx" in n:
            score += 16
        if "dry" in n or "raw" in n:
            score -= 7
        if any(h in n for h in VOICE_HINTS):
            score += 12
        if any(h in n for h in BAD_HINTS):
            score -= 40
        best = max(best, score)
    return best


def list_drive():
    result = run(["gdown", BUNDLE_URL, "--folder", "--json", "--quiet"], capture=True)
    entries = json.loads(result.stdout)
    (PREVIEW / "drive_listing.json").write_text(json.dumps(entries, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"Drive listing contains {len(entries)} files", flush=True)
    return entries


def choose_drive_entry(entries, phrases):
    candidates = []
    for e in entries:
        path = e.get("path", "")
        ext = Path(path).suffix.lower()
        if ext not in AUDIO_EXTS:
            continue
        sc = score_name(path, phrases)
        if sc > 0:
            candidates.append((sc, path, e["url"]))
    candidates.sort(reverse=True)
    return candidates[0] if candidates else None


def download_entry(url: str, dest: Path):
    run(["gdown", url, "-O", str(dest), "--quiet", "--no-cookies"])
    return dest


def category_archives(entries):
    found = []
    for e in entries:
        path = e.get("path", "")
        n = norm(path)
        if Path(path).suffix.lower() != ".zip":
            continue
        if any(h in n for h in ("female ghost", "creepy girl", "ghostly", "evil women", "dark witch")):
            score = 0
            if "female ghost" in n:
                score += 30
            if "creepy girl" in n:
                score += 24
            if "ghostly" in n:
                score += 18
            if "free" in n:
                score += 4
            found.append((score, path, e["url"]))
    found.sort(reverse=True)
    return found


def unpack_candidate_archives(entries):
    root = TMP / "archives"
    root.mkdir(parents=True, exist_ok=True)
    chosen = category_archives(entries)[:4]
    if not chosen:
        return []
    print("Downloading a few voice-category archives as fallback:", flush=True)
    for i, (_, path, url) in enumerate(chosen):
        print("  ", path, flush=True)
        zpath = TMP / f"category_{i}.zip"
        try:
            download_entry(url, zpath)
            with zipfile.ZipFile(zpath) as z:
                z.extractall(root / f"cat_{i}")
        except Exception as ex:
            print(f"Archive fallback failed for {path}: {ex}", flush=True)
    return [p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in AUDIO_EXTS]


def choose_local(files, phrases):
    candidates = []
    for p in files:
        sc = score_name(str(p), phrases)
        if sc > 0:
            candidates.append((sc, len(str(p)), p))
    candidates.sort(key=lambda x: (-x[0], x[1], str(x[2])))
    return candidates[0][2] if candidates else None


def make_silence(dest_ogg: Path):
    run(["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono", "-t", "0.25", "-c:a", "libvorbis", "-q:a", "5", str(dest_ogg)])


def process_voice(src: Path, dest_ogg: Path):
    # Keep the real performance intact. Only trim dead air, remove low rumble and normalize gently.
    filt = (
        "silenceremove=start_periods=1:start_silence=0.03:start_threshold=-52dB:"
        "stop_periods=-1:stop_silence=0.40:stop_threshold=-55dB,"
        "highpass=f=80,lowpass=f=7200,"
        "acompressor=threshold=-24dB:ratio=1.45:attack=12:release=220,volume=1.04"
    )
    run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(src), "-af", filt, "-c:a", "libvorbis", "-q:a", "6", str(dest_ogg)])


def main():
    entries = list_drive()
    local_fallback = None
    selected = {}

    # First pass: use individually exposed audio files from the public Drive bundle.
    for event, phrases in TARGETS.items():
        pick = choose_drive_entry(entries, phrases)
        if pick:
            sc, path, url = pick
            src = TMP / f"{event}.source"
            try:
                download_entry(url, src)
                selected[event] = (src, path, sc)
                continue
            except Exception as ex:
                print(f"Direct file download failed for {event}: {ex}", flush=True)

    # If the Drive exposes category ZIPs instead of individual files, download only a few relevant packs.
    missing = [e for e in TARGETS if e not in selected]
    if missing:
        local_fallback = unpack_candidate_archives(entries)
        for event in missing:
            p = choose_local(local_fallback, TARGETS[event]) if local_fallback else None
            if p:
                selected[event] = (p, str(p), score_name(str(p), TARGETS[event]))

    # Find one real human clip as a last-resort source only for preview reference.
    # Missing gameplay lines are SILENT instead of falling back to robotic TTS or repeating one sentence.
    human_reference = None
    for preferred in ("whisper_injaa", "whisper_here", "whisper_run"):
        if preferred in selected:
            human_reference = selected[preferred][0]
            break

    manifest = []
    preview_inputs = []
    for event in TARGETS:
        out = OUT / f"{event}.ogg"
        if event in selected:
            src, label, sc = selected[event]
            process_voice(src, out)
            manifest.append(f"{event}\tHUMAN\t{label}\tscore={sc}")
            preview_inputs.append(out)
        else:
            make_silence(out)
            manifest.append(f"{event}\tSILENT\t(no matching human phrase found; AI/TTS intentionally disabled)")

    manifest_path = PREVIEW / "Train31_Human_Voice_Selection.txt"
    manifest_path.write_text("\n".join(manifest) + "\n", encoding="utf-8")
    print(manifest_path.read_text(encoding="utf-8"), flush=True)

    # Build an MP3 preview from the actual OGG files that Minecraft will use.
    playable = [p for p in preview_inputs if p.exists()]
    if playable:
        concat_file = TMP / "preview_concat.txt"
        concat_file.write_text("\n".join(f"file '{p.resolve()}'" for p in playable), encoding="utf-8")
        run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(concat_file), "-c:a", "libmp3lame", "-q:a", "2", str(PREVIEW / "Train31_Real_Human_Voice_Preview.mp3")])
    elif human_reference:
        run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(human_reference), "-c:a", "libmp3lame", "-q:a", "2", str(PREVIEW / "Train31_Real_Human_Voice_Preview.mp3")])
    else:
        raise RuntimeError("Could not locate even one real female horror voice in the licensed public bundle.")


if __name__ == "__main__":
    main()
