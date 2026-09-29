"""Pack the block map into a standalone Java 1.20.1 Anvil world.

This intentionally writes real block states to chunks, not a screenshot or
commands that replace an existing world. Run with PYTHONPATH=pydeps.
"""
from __future__ import annotations
import io, json, math, os, runpy, struct, time, zlib
from pathlib import Path
from nbtlib import Byte, Compound, Int, List, Long, LongArray, String, File, load

ROOT=Path(__file__).resolve().parent
S=runpy.run_path(str(ROOT/'build_map.py'))['S']
MODDED=os.environ.get('ROOM203_MACAW','0')=='1'
WORLD=ROOT/('Room203_Japanese_Horror_Town_1.20.1_MacawsFurniture' if MODDED
            else 'Room203_Japanese_Horror_Town_1.20.1')
REGION=WORLD/'region'
REGION.mkdir(parents=True,exist_ok=True)
Y_OFFSET=64

def state(value:str)->Compound:
    if '[' not in value:return Compound({'Name':String(value)})
    name,raw=value[:-1].split('[',1)
    return Compound({'Name':String(name),'Properties':Compound(dict(
        (k,String(v)) for k,v in (part.split('=',1) for part in raw.split(','))))})

def signed(value:int)->int:return value if value<1<<63 else value-(1<<64)

def pack(values:list[int],bits:int)->LongArray:
    per=64//bits; result=[]
    for start in range(0,len(values),per):
        n=0
        for offset,value in enumerate(values[start:start+per]):n|=value<<(offset*bits)
        result.append(signed(n))
    return LongArray(result)

def block(x:int,y:int,z:int)->str:
    if y==-64:return 'minecraft:bedrock'
    if y<=60:return 'minecraft:stone'
    if y<=63:return 'minecraft:dirt'
    if y==64 and (x>=S.w or z>=S.d):return 'minecraft:grass_block'
    local_y=y-Y_OFFSET
    if 0<=x<S.w and 0<=z<S.d and 0<=local_y<S.h:
        return S.blocks[(local_y*S.d+z)*S.w+x]
    return 'minecraft:air'

def section(cx:int,cz:int,sy:int)->Compound:
    names=[];indices=[];lookup={}
    for ly in range(16):
        y=sy*16+ly
        for lz in range(16):
            for lx in range(16):
                name=block(cx*16+lx,y,cz*16+lz)
                if name not in lookup:lookup[name]=len(names);names.append(name)
                indices.append(lookup[name])
    states=Compound({'palette':List[Compound]([state(n) for n in names])})
    if len(names)>1:states['data']=pack(indices,max(4,(len(names)-1).bit_length()))
    return Compound({'Y':Byte(sy),'block_states':states,
                     'biomes':Compound({'palette':List[String]([String('minecraft:plains')])})})

def chunk(cx:int,cz:int)->bytes:
    sections=List[Compound]([section(cx,cz,sy) for sy in range(-4,6)])
    heights=[]
    for z in range(cz*16,cz*16+16):
        for x in range(cx*16,cx*16+16):
            top=64
            for y in range(92,64,-1):
                if block(x,y,z)!='minecraft:air':top=y;break
            heights.append(top+1+64)
    maps=Compound({key:pack(heights,9) for key in
        ('WORLD_SURFACE','MOTION_BLOCKING','MOTION_BLOCKING_NO_LEAVES','OCEAN_FLOOR')})
    signs=[]
    for x,z,label in ((58,44,'203'),(58,51,'204')):
        if x//16==cx and z//16==cz:
            lines=[json.dumps({'text':line}) for line in (label,'',
                   'YOUR ROOM' if label=='203' else 'VACANT','')]
            front=Compound({'messages':List[String](map(String,lines)),
                 'filtered_messages':List[String](map(String,lines)),
                 'color':String('black'),'has_glowing_text':Byte(1)})
            signs.append(Compound({'id':String('minecraft:sign'),
                'x':Int(x),'y':Int(Y_OFFSET+8),'z':Int(z),'front_text':front,
                'back_text':front,'is_waxed':Byte(1)}))
    root=Compound({'DataVersion':Int(3465),'xPos':Int(cx),'zPos':Int(cz),'yPos':Int(-4),
        'Status':String('minecraft:full'),'LastUpdate':Long(0),'InhabitedTime':Long(0),
        'isLightOn':Byte(0),'sections':sections,'Heightmaps':maps,
        'block_entities':List[Compound](signs),'block_ticks':List[Compound]([]),
        'fluid_ticks':List[Compound]([]),'PostProcessing':List[List[Int]]([]),
        'structures':Compound({'starts':Compound({}),'References':Compound({})})})
    stream=io.BytesIO();File(root,root_name='').write(stream)
    return zlib.compress(stream.getvalue(),6)

header=bytearray(8192);payload=bytearray();sector=2
for cz in range((S.d+15)//16):
    for cx in range((S.w+15)//16):
        raw=chunk(cx,cz);record=struct.pack('>I',len(raw)+1)+b'\x02'+raw
        count=(len(record)+4095)//4096
        index=cx+cz*32
        header[index*4:index*4+4]=(sector<<8|count).to_bytes(4,'big')
        header[4096+index*4:4100+index*4]=int(time.time()).to_bytes(4,'big')
        payload.extend(record);payload.extend(bytes(count*4096-len(record)))
        sector+=count
(REGION/'r.0.0.mca').write_bytes(header+payload)

# Use 1.20.1 metadata as a skeleton, then replace all world identity/terrain.
source=Path(__file__).resolve().parents[1]/'world/Tokyo Inspired City 1.0.10/level.dat'
level=load(source)
d=level['Data']
d.pop('Player',None)
d['LevelName']=String('Room 203 - Japanese Horror Town' + (' Macaw' if MODDED else ''))
d['SpawnX']=Int(44);d['SpawnY']=Int(65);d['SpawnZ']=Int(92)
d['SpawnAngle']=__import__('nbtlib').Float(180)
d['GameType']=Int(1);d['allowCommands']=Byte(1)
d['DayTime']=Long(18000);d['Time']=Long(18000)
d['raining']=Byte(1);d['rainTime']=Int(999999)
d['thundering']=Byte(0)
d['GameRules']['doDaylightCycle']=String('false')
d['GameRules']['doWeatherCycle']=String('false')
d['GameRules']['doMobSpawning']=String('false')
d['WorldGenSettings']['seed']=Long(203)
d['WorldGenSettings']['generate_features']=Byte(0)
d['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']=Compound({
    'type':String('minecraft:flat'),'settings':Compound({
        'biome':String('minecraft:plains'),'features':Byte(0),'lakes':Byte(0),
        'layers':List[Compound]([
            Compound({'height':Int(1),'block':String('minecraft:bedrock')}),
            Compound({'height':Int(125),'block':String('minecraft:stone')}),
            Compound({'height':Int(2),'block':String('minecraft:dirt')}),
            Compound({'height':Int(1),'block':String('minecraft:grass_block')})])})})
level.save(WORLD/'level.dat',gzipped=True)
(WORLD/'session.lock').write_bytes(struct.pack('>q',int(time.time()*1000)))
print('Saved',WORLD,'region bytes',(REGION/'r.0.0.mca').stat().st_size)
