#!/usr/bin/env python3
import json
import re
import subprocess
import zipfile
from pathlib import Path

BUNDLE_URL = "https://drive.google.com/drive/folders/1RXNykUKtSTMwirqxyIWxNneDqrwghHUj?usp=sharing"
TMP = Path("/tmp/train31_human_voices")
TMP.mkdir(parents=True, exist_ok=True)
OUT = Path("src/main/resources/assets/train31/sounds")
OUT.mkdir(parents=True, exist_ok=True)
PREVIEW = Path("build/preview")
PREVIEW.mkdir(parents=True, exist_ok=True)

TARGETS = {
    "whisper_injaa": [("been waiting for you",), ("waiting for you",), ("hello",)],
    "whisper_can_see": [("can you see me",), ("can see you",), ("see you",), ("i know you are here",)],
    "whisper_behind": [("dont turn around",), ("don't turn around",), ("behind you",), ("beware",)],
    "whisper_here": [("shouldnt be here",), ("shouldn't be here",), ("should not be here",), ("i know you are here",)],
    "whisper_why_here": [("why did you come",), ("why are you here",), ("come to me",), ("follow me",)],
    "whisper_coming": [("its coming",), ("it's coming",), ("coming",), ("follow me",)],
    "whisper_dont_board": [("dont get on",), ("don't get on",), ("dont board",), ("don't board",), ("beware",)],
    "whisper_see_you": [("i can see you",), ("can see you",), ("i know you are here",)],
    "whisper_cant_leave": [("cant leave",), ("can't leave",), ("cannot leave",), ("waiting for you",)],
    "whisper_run": [("run",), ("get out",), ("leave",)],
    "whisper_found_you": [("i found you",), ("found you",), ("i know you are here",), ("waiting for you",)],
    "whisper_should_listen": [("should have listened",), ("you should listen",), ("listen",), ("beware",)],
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
    best = -10000
    for (phrase,) in phrases:
        p = norm(phrase)
        if p in n:
            score = 100 + len(p)
        else:
            words = [w for w in p.split() if len(w) > 1]
            if not words or any(w not in n for w in words):
                continue
            score = 40 + len(words) * 6
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
    for e in entries:
        print("DRIVE_FILE:", e.get("path", ""), "=>", e.get("url", ""), flush=True)
    return entries


def choose_drive_entry(entries, phrases):
    candidates = []
    for e in entries:
        path = e.get("path", "")
        if Path(path).suffix.lower() not in AUDIO_EXTS:
            continue
        sc = score_name(path, phrases)
        if sc > 0:
            candidates.append((sc, path, e["url"]))
    candidates.sort(reverse=True)
    return candidates[0] if candidates else None


def download_entry(url: str, dest: Path):
    run(["gdown", url, "-O", str(dest), "--quiet", "--no-cookies"])
    return dest


def archive_candidates(entries):
    found = []
    for e in entries:
        path = e.get("path", "")
        if Path(path).suffix.lower() != ".zip":
            continue
        n = norm(path)
        score = 0
        if "female ghost" in n: score += 50
        if "creepy girl" in n: score += 45
        if "ghostly" in n: score += 35
        if "free" in n: score += 25
        if "horror" in n: score += 15
        if "bundle" in n: score += 10
        if "update" in n: score -= 5
        found.append((score, path, e["url"]))
    found.sort(reverse=True)
    return found


def unpack_candidate_archives(entries):
    root = TMP / "archives"
    root.mkdir(parents=True, exist_ok=True)
    chosen = archive_candidates(entries)[:2]
    if not chosen:
        return []
    print("Trying top archive fallbacks:", flush=True)
    for i, (_, path, url) in enumerate(chosen):
        print("ARCHIVE:", path, flush=True)
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
    filt = (
        "silenceremove=start_periods=1:start_silence=0.03:start_threshold=-52dB:"
        "stop_periods=-1:stop_silence=0.40:stop_threshold=-55dB,"
        "highpass=f=80,lowpass=f=7200,"
        "acompressor=threshold=-24dB:ratio=1.45:attack=12:release=220,volume=1.04"
    )
    run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(src), "-af", filt, "-c:a", "libvorbis", "-q:a", "6", str(dest_ogg)])


def main():
    entries = list_drive()
    selected = {}

    for event, phrases in TARGETS.items():
        pick = choose_drive_entry(entries, phrases)
        if pick:
            sc, path, url = pick
            src = TMP / f"{event}.source"
            try:
                download_entry(url, src)
                selected[event] = (src, path, sc)
            except Exception as ex:
                print(f"Direct file download failed for {event}: {ex}", flush=True)

    missing = [e for e in TARGETS if e not in selected]
    local_files = unpack_candidate_archives(entries) if missing else []
    for event in missing:
        p = choose_local(local_files, TARGETS[event]) if local_files else None
        if p:
            selected[event] = (p, str(p), score_name(str(p), TARGETS[event]))

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

    if preview_inputs:
        concat_file = TMP / "preview_concat.txt"
        concat_file.write_text("\n".join(f"file '{p.resolve()}'" for p in preview_inputs), encoding="utf-8")
        run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(concat_file), "-c:a", "libmp3lame", "-q:a", "2", str(PREVIEW / "Train31_Real_Human_Voice_Preview.mp3")])
    else:
        raise RuntimeError("Could not locate a real female horror voice in the public bundle; see DRIVE_FILE lines above.")


if __name__ == "__main__":
    main()
