"""Install the original Room 203 district into the user's Tokyo 1.20.1 world.

Requires nbtlib. The Tokyo map is a user-supplied input and is never committed
or redistributed with this source. The modified copy stays in the local output.
"""
from __future__ import annotations

import io
import json
import os
import runpy
import shutil
import struct
import sys
import time
import zlib
from pathlib import Path

from nbtlib import Byte, Compound, Double, File, Float, Int, List, Long, LongArray, String, load

ROOT = Path(__file__).resolve().parent
SOURCE = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT.parent / 'world/Tokyo Inspired City 1.0.10'
MODDED = os.environ.get('ROOM203_MACAW', '0') == '1'
NAME = 'Room203_Tokyo_4km_Macaw_1.20.1_v6' if MODDED else 'Room203_Tokyo_4km_1.20.1_v6'
DEST = ROOT / NAME
ORIGIN_X, ORIGIN_Y, ORIGIN_Z = -700, 62, -350

if not (SOURCE / 'level.dat').exists():
    raise SystemExit('Source Tokyo world folder not found: ' + str(SOURCE))
if DEST.exists():
    raise SystemExit('Output folder already exists; move it away before rebuilding: ' + str(DEST))

S = runpy.run_path(str(ROOT / 'build_map_v3.py'))['S']
print('Copying user-supplied world ...', flush=True)
shutil.copytree(SOURCE, DEST, ignore=shutil.ignore_patterns('session.lock', 'uid.dat'))

def signed(value: int) -> int:
    return value if value < 1 << 63 else value - (1 << 64)

def pack(values: list[int], bits: int) -> LongArray:
    per = 64 // bits
    return LongArray([signed(sum(int(v) << (j * bits) for j, v in enumerate(values[i:i+per])))
                      for i in range(0, len(values), per)])

def unpack(values: LongArray, bits: int, length: int) -> list[int]:
    per = 64 // bits
    nums = [int(v) & ((1 << 64) - 1) for v in values]
    mask = (1 << bits) - 1
    return [(nums[i // per] >> ((i % per) * bits)) & mask for i in range(length)]

def compound_state(value: str) -> Compound:
    if '[' not in value:
        return Compound({'Name': String(value)})
    name, props = value[:-1].split('[', 1)
    return Compound({'Name': String(name), 'Properties': Compound({
        key: String(val) for key, val in (pair.split('=', 1) for pair in props.split(','))})})

def state_key(state: Compound) -> str:
    name = str(state['Name'])
    props = state.get('Properties')
    if not props:
        return name
    return name + '[' + ','.join(f'{key}={val}' for key, val in sorted(props.items())) + ']'

def name_key(name: str) -> str:
    return state_key(compound_state(name))

def patch_section(section: Compound, cx: int, cz: int, sy: int) -> None:
    block_states = section.get('block_states', Compound())
    palette = list(block_states.get('palette', List[Compound]([compound_state('minecraft:air')])))
    old_bits = max(4, (len(palette) - 1).bit_length())
    values = (unpack(block_states['data'], old_bits, 4096) if 'data' in block_states
              else [0] * 4096)
    lookup = {state_key(state): i for i, state in enumerate(palette)}
    x0, x1 = max(cx*16, ORIGIN_X), min(cx*16+15, ORIGIN_X+S.w-1)
    z0, z1 = max(cz*16, ORIGIN_Z), min(cz*16+15, ORIGIN_Z+S.d-1)
    y0, y1 = max(sy*16, ORIGIN_Y), min(sy*16+15, ORIGIN_Y+S.h-1)
    for y in range(y0, y1+1):
        ly = y - ORIGIN_Y
        for z in range(z0, z1+1):
            lz = z - ORIGIN_Z
            for x in range(x0, x1+1):
                name = S.blocks[(ly*S.d+lz)*S.w+(x-ORIGIN_X)]
                key = name_key(name)
                index = lookup.get(key)
                if index is None:
                    index = len(palette)
                    lookup[key] = index
                    palette.append(compound_state(name))
                values[((y % 16)*16+(z % 16))*16+x % 16] = index
    block_states['palette'] = List[Compound](palette)
    if len(palette) > 1:
        block_states['data'] = pack(values, max(4, (len(palette)-1).bit_length()))
    else:
        block_states.pop('data', None)
    section['block_states'] = block_states

def make_sign(x: int, z: int, label: str) -> Compound:
    lines = [json.dumps({'text': line}) for line in (label, '', 'YOUR ROOM' if label == '203' else 'VACANT', '')]
    front = Compound({'messages': List[String](map(String, lines)),
                      'filtered_messages': List[String](map(String, lines)),
                      'color': String('black'), 'has_glowing_text': Byte(1)})
    return Compound({'id': String('minecraft:sign'), 'x': Int(x), 'y': Int(ORIGIN_Y+8),
                     'z': Int(z), 'front_text': front, 'back_text': front, 'is_waxed': Byte(1)})

def patch_chunk(raw_record: bytes, cx: int, cz: int) -> bytes:
    size = struct.unpack('>I', raw_record[:4])[0]
    if raw_record[4] != 2:
        raise ValueError('Unsupported chunk compression at ' + str((cx, cz)))
    chunk = File.parse(io.BytesIO(zlib.decompress(raw_record[5:4+size])))
    sections = {int(sec['Y']): sec for sec in chunk.get('sections', [])}
    for sy in range(ORIGIN_Y//16, (ORIGIN_Y+S.h-1)//16+1):
        sec = sections.get(sy)
        if sec is None:
            sec = Compound({'Y': Byte(sy), 'block_states': Compound({
                'palette': List[Compound]([compound_state('minecraft:air')])}),
                'biomes': Compound({'palette': List[String]([String('minecraft:plains')])})})
            chunk['sections'].append(sec)
            sections[sy] = sec
        patch_section(sec, cx, cz, sy)
    # Keep the original terrain's heightmap outside our district footprint.
    x0, x1 = max(cx*16, ORIGIN_X), min(cx*16+15, ORIGIN_X+S.w-1)
    z0, z1 = max(cz*16, ORIGIN_Z), min(cz*16+15, ORIGIN_Z+S.d-1)
    for key, encoded in chunk.get('Heightmaps', {}).items():
        values = unpack(encoded, 9, 256)
        for z in range(z0, z1+1):
            for x in range(x0, x1+1):
                lx, lz = x-ORIGIN_X, z-ORIGIN_Z
                highest = max((y for y in range(S.h-1, -1, -1)
                               if S.blocks[(y*S.d+lz)*S.w+lx] != 'minecraft:air'), default=0)
                values[(z % 16)*16+x % 16] = ORIGIN_Y+highest+1+64
        chunk['Heightmaps'][key] = pack(values, 9)
    existing = [e for e in chunk.get('block_entities', [])
                if not (ORIGIN_X <= int(e['x']) < ORIGIN_X+S.w and
                        ORIGIN_Z <= int(e['z']) < ORIGIN_Z+S.d and
                        ORIGIN_Y <= int(e['y']) < ORIGIN_Y+S.h)]
    for lx, lz, text in ((58,44,'203'), (58,51,'204')):
        x, z = ORIGIN_X+lx, ORIGIN_Z+lz
        if x//16 == cx and z//16 == cz:
            existing.append(make_sign(x, z, text))
    chunk['block_entities'] = List[Compound](existing)
    chunk['isLightOn'] = Byte(0)
    buf = io.BytesIO()
    chunk.write(buf)
    payload = zlib.compress(buf.getvalue(), 6)
    return struct.pack('>I', len(payload)+1) + b'\x02' + payload

region_paths = sorted({DEST/'region'/f'r.{cx//32}.{cz//32}.mca'
                       for cx in range(ORIGIN_X//16, (ORIGIN_X+S.w-1)//16+1)
                       for cz in range(ORIGIN_Z//16, (ORIGIN_Z+S.d-1)//16+1)})
patched = 0
for path in region_paths:
    _, xs, zs, _ = path.name.split('.')
    rx, rz = int(xs), int(zs)
    original = path.read_bytes()
    header = bytearray(8192)
    data = bytearray()
    sector = 2
    for i in range(1024):
        old = int.from_bytes(original[4*i:4*i+4], 'big')
        if not old:
            continue
        pos = (old >> 8)*4096
        size = struct.unpack('>I', original[pos:pos+4])[0]
        record = original[pos:pos+4+size]
        cx, cz = rx*32+i%32, rz*32+i//32
        if (ORIGIN_X <= cx*16+15 and cx*16 < ORIGIN_X+S.w and
                ORIGIN_Z <= cz*16+15 and cz*16 < ORIGIN_Z+S.d):
            record = patch_chunk(record, cx, cz)
            patched += 1
        sectors = (len(record)+4095)//4096
        if sectors > 255:
            raise ValueError('Chunk too large at ' + str((cx,cz)))
        header[4*i:4*i+4] = (sector << 8 | sectors).to_bytes(4, 'big')
        header[4096+4*i:4100+4*i] = original[4096+4*i:4100+4*i]
        data.extend(record)
        data.extend(bytes(sectors*4096-len(record)))
        sector += sectors
    path.write_bytes(header+data)
    print('Patched region', path.name, flush=True)

level = load(DEST/'level.dat')
d = level['Data']
d.pop('Player', None)
d['LevelName'] = String('Room 203 - Tokyo 4 km district' + (' + Furniture' if MODDED else ''))
d['SpawnX'], d['SpawnY'], d['SpawnZ'] = Int(ORIGIN_X+44), Int(ORIGIN_Y+1), Int(ORIGIN_Z+92)
d['SpawnAngle'] = Float(180)
d['GameType'] = Int(1)
d['allowCommands'] = Byte(1)
d['DayTime'], d['Time'] = Long(18000), Long(18000)
d['raining'], d['rainTime'] = Byte(1), Int(999999)
d['GameRules']['doDaylightCycle'] = String('false')
d['GameRules']['doWeatherCycle'] = String('false')
d['BorderCenterX'], d['BorderCenterZ'] = Double(0), Double(1024)
d['BorderSize'], d['BorderSizeLerpTarget'] = Double(4096), Double(4096)
level.save(DEST/'level.dat', gzipped=True)
(DEST/'session.lock').write_bytes(struct.pack('>q', int(time.time()*1000)))
print('Saved', DEST, 'patched chunks', patched, 'spawn', (ORIGIN_X+44, ORIGIN_Y+1, ORIGIN_Z+92))
