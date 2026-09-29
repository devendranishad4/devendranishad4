"""Generate standalone WorldEdit schematics for The Last Elevator (Sponge v2)."""
from collections import Counter
from pathlib import Path
import nbtlib
from nbtlib import ByteArray, Compound, Int, IntArray, List, Short, String
from PIL import Image, ImageDraw

OUT = Path('LastElevatorBuild/sets')
OUT.mkdir(parents=True, exist_ok=True)

class Set:
    def __init__(self, name, w, h, d):
        self.name, self.w, self.h, self.d = name, w, h, d
        self.blocks = ['minecraft:air'] * (w*h*d)
    def put(self,x,y,z,b):
        if 0<=x<self.w and 0<=y<self.h and 0<=z<self.d:
            self.blocks[(y*self.d+z)*self.w+x]=b
    def fill(self,x1,y1,z1,x2,y2,z2,b):
        for y in range(y1,y2+1):
            for z in range(z1,z2+1):
                for x in range(x1,x2+1):self.put(x,y,z,b)
    def line(self,x1,y1,z1,x2,y2,z2,b):self.fill(x1,y1,z1,x2,y2,z2,b)
    def box(self,x1,y1,z1,x2,y2,z2,wall,floor,ceiling):
        self.fill(x1,y1,z1,x2,y1,z2,floor)
        self.fill(x1,y2,z1,x2,y2,z2,ceiling)
        for y in range(y1+1,y2):
            for z in range(z1,z2+1):
                self.put(x1,y,z,wall);self.put(x2,y,z,wall)
            for x in range(x1,x2+1):
                self.put(x,y,z1,wall);self.put(x,y,z2,wall)
    def door(self,x,y,z,axis='z',width=3,height=4):
        self.fill(x,y,z,x+(width-1 if axis=='x' else 0),y+height-1,z+(width-1 if axis=='z' else 0),'minecraft:air')
    def export(self):
        palette={}
        data=bytearray()
        for b in self.blocks:
            if b not in palette:palette[b]=len(palette)
            n=palette[b]
            while n>127:
                data.append((n&127)|128);n>>=7
            data.append(n)
        root=Compound({
            'Version':Int(2),'DataVersion':Int(3465),
            'Width':Short(self.w),'Height':Short(self.h),'Length':Short(self.d),
            'Offset':IntArray([0,0,0]),'PaletteMax':Int(len(palette)),
            'Palette':Compound({k:Int(v) for k,v in palette.items()}),
            'BlockData':ByteArray([i if i<128 else i-256 for i in data]),
            'BlockEntities':List[Compound]([]),'Entities':List[Compound]([]),
            'Metadata':Compound({'Name':String(self.name)})})
        nbtlib.File(root,root_name='Schematic').save(OUT/(self.name+'.schem'),gzipped=True)
        self.preview()
        print(self.name,self.w,self.h,self.d,len(palette),Counter(self.blocks).most_common(4))
    def preview(self):
        colors={'air':(15,18,24),'glass':(40,92,120),'light':(230,190,105),'lamp':(230,190,105),'door':(110,72,48),'concrete':(160,160,155),'quartz':(222,221,211),'stone':(85,87,91),'bricks':(115,67,52),'carpet':(90,23,23),'copper':(128,93,68),'iron':(120,133,142),'wood':(98,64,40)}
        im=Image.new('RGB',(self.w,self.d));p=im.load()
        for z in range(self.d):
            for x in range(self.w):
                b=self.blocks[(2*self.d+z)*self.w+x]
                color=(65,65,65)
                for key,v in colors.items():
                    if key in b:color=v;break
                p[x,z]=color
        im.resize((self.w*12,self.d*12),Image.Resampling.NEAREST).save(OUT/(self.name+'_plan.png'))

def lights(s,y,coords,block='minecraft:sea_lantern'):
    for x,z in coords:s.fill(x,y,z,x+1,y,z+1,block)

def lift(s,cx,cz,y=1):
    # 7x7 self-contained car; opening faces south (towards decreasing z).
    s.box(cx,y-1,cz,cx+6,y+6,cz+6,'minecraft:polished_deepslate','minecraft:polished_blackstone','minecraft:polished_deepslate')
    s.fill(cx+1,y,cz+1,cx+5,y+4,cz+5,'minecraft:air')
    s.fill(cx+1,y,cz,cx+5,y+3,cz,'minecraft:air')
    s.fill(cx+1,y,cz+1,cx+5,y+3,cz+1,'minecraft:iron_block')
    s.fill(cx+1,y,cz+1,cx+5,y+3,cz+1,'minecraft:air')
    for xx in (cx+1,cx+5):s.fill(xx,y,cz+2,xx,y+3,cz+5,'minecraft:iron_block')
    lights(s,y+5,[(cx+2,cz+2),(cx+4,cz+2)])
    s.put(cx+5,y+2,cz+2,'minecraft:stone_button[face=wall,facing=west,powered=false]')

def luxury_lobby(s):
    # Dark marble grid, pale stone inlay, and a clear route from the door to the lift.
    for x in range(2,44):
        for z in range(4,36):
            s.put(x,0,z,'minecraft:polished_blackstone' if (x//5+z//5)%2 else 'minecraft:polished_diorite')
    for x in (2,3,42,43):
        s.fill(x,0,4,x,0,35,'minecraft:polished_blackstone')
    for z in (4,5,34,35):
        s.fill(2,0,z,43,0,z,'minecraft:polished_blackstone')
    s.fill(21,0,4,26,0,26,'minecraft:polished_deepslate')
    for z in range(5,27,4):s.fill(23,0,z,24,0,z+1,'minecraft:chiseled_quartz_block')
    # Tall fluted columns, bronze trim, warm sconces and layered ceiling coves.
    for x,z in ((3,7),(3,32),(15,7),(15,32),(28,7),(28,32),(42,7),(42,32)):
        s.fill(x,1,z,x+1,9,z+1,'minecraft:smooth_quartz')
        s.fill(x,1,z,x+1,1,z+1,'minecraft:polished_blackstone')
        s.fill(x,9,z,x+1,9,z+1,'minecraft:polished_blackstone')
        s.put(x,6,z,'minecraft:shroomlight')
    for y in (2,8):
        s.fill(2,y,35,43,y,35,'minecraft:cut_copper')
        s.fill(2,y,4,18,y,4,'minecraft:cut_copper')
        s.fill(27,y,4,43,y,4,'minecraft:cut_copper')
    for x in range(4,43,7):
        s.fill(x,10,6,x+3,10,6,'minecraft:ochre_froglight')
        s.fill(x,10,34,x+3,10,34,'minecraft:ochre_froglight')
    for z in (9,17,25,33):
        s.fill(5,10,z,40,10,z,'minecraft:dark_oak_planks')
        s.fill(8,10,z,11,10,z,'minecraft:ochre_froglight')
        s.fill(29,10,z,32,10,z,'minecraft:ochre_froglight')
    # Reception with a glowing counter lip, columns behind it, and luggage storage.
    s.fill(6,1,20,18,2,22,'minecraft:polished_blackstone_bricks')
    s.fill(6,3,19,18,3,22,'minecraft:dark_oak_planks')
    s.fill(6,2,19,18,2,19,'minecraft:ochre_froglight')
    for x in (8,12,16):
        s.fill(x,1,31,x+1,7,31,'minecraft:stripped_dark_oak_log')
        s.fill(x,8,30,x+1,8,32,'minecraft:cut_copper')
    # Two seating islands with low tables; their middle remains walkable.
    for x,z in ((7,11),(9,28),(32,12)):
        s.fill(x,1,z,x+4,1,z,'minecraft:dark_oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]')
        s.fill(x+1,1,z+3,x+3,1,z+3,'minecraft:dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
        s.fill(x+1,1,z+1,x+3,1,z+2,'minecraft:brown_carpet')
        s.fill(x+2,1,z+1,x+2,1,z+2,'minecraft:polished_blackstone_slab[type=bottom,waterlogged=false]')
    for x,z in ((4,17),(19,33),(30,6),(41,21)):
        s.fill(x,1,z,x+2,2,z+2,'minecraft:polished_blackstone')
        s.fill(x+1,3,z+1,x+1,5,z+1,'minecraft:oak_leaves[persistent=true,distance=1]')
    # Full-height glass frontage framed by deep stone mullions.
    for x in range(2,44):
        if not 19<=x<=26:s.fill(x,1,3,x,9,3,'minecraft:tinted_glass')
    for x in (2,10,18,27,35,43):s.fill(x,1,3,x,10,3,'minecraft:polished_deepslate')
    s.fill(20,1,3,25,4,3,'minecraft:air')
    s.fill(1,0,0,44,0,2,'minecraft:polished_deepslate')
    for x in (8,36):s.fill(x,1,1,x+1,7,1,'minecraft:sea_lantern')

def detailed_hotel(s):
    # Preserve the central x-axis corridor and turn both sides into repeated suites.
    for x in range(1,51):
        for z in range(7,13):
            s.put(x,0,z,'minecraft:red_carpet' if 8<=z<=11 else 'minecraft:polished_blackstone')
        if x%9 in (0,1):
            for z in (7,12):s.fill(x,1,z,x,5,z,'minecraft:stripped_dark_oak_log')
        if x%9 in (3,4,5):
            for z in (7,12):s.fill(x,2,z,x,3,z,'minecraft:dark_oak_planks')
    for x in range(4,49,9):
        for z in (7,12):
            s.fill(x,1,z,x+2,3,z,'minecraft:air')
            s.fill(x,4,z,x+2,4,z,'minecraft:cut_copper')
            s.put(x+1,5,z,'minecraft:ochre_froglight')
            s.fill(x+1,1,z+(-1 if z==7 else 1),x+1,1,z+(-1 if z==7 else 1),'minecraft:dark_oak_planks')
        s.fill(x,6,8,x+2,6,11,'minecraft:ochre_froglight')
    # Decaying but once-grand rooms: beds, paintings, mirrored wall and trolley.
    for x in (12,21,30,39):
        for z in (3,16):
            s.fill(x,0,z,x+4,0,z+1,'minecraft:dark_oak_planks')
            s.fill(x,1,z,x+4,1,z+1,'minecraft:white_wool')
            s.fill(x+1,2,z,x+3,2,z,'minecraft:red_carpet')
    for x in (15,33,45):
        s.fill(x,1,3,x,2,3,'minecraft:dark_oak_fence')
        s.put(x,3,3,'minecraft:lantern')

def detailed_maintenance(s):
    for x in range(2,50):
        s.fill(x,0,7,x,0,12,'minecraft:polished_deepslate' if x%7 else 'minecraft:oxidized_copper')
        for z in (6,13):
            if x%7 in (0,1):s.fill(x,1,z,x,5,z,'minecraft:polished_blackstone_bricks')
            if x%7 in (3,4):s.fill(x,2,z,x,3,z,'minecraft:iron_bars')
        if x%8==0:
            s.fill(x,6,7,x+2,6,7,'minecraft:oxidized_copper')
            s.fill(x,6,12,x+2,6,12,'minecraft:oxidized_copper')
            s.put(x,5,8,'minecraft:redstone_lamp[lit=true]')
    for x in (12,24,35):
        s.fill(x,1,2,x+2,2,4,'minecraft:barrel[facing=up,open=false]')
        s.fill(x,1,15,x+2,2,17,'minecraft:iron_block')

def detailed_zero(s):
    for x in range(1,51):
        for z in range(6,14):
            s.put(x,0,z,'minecraft:smooth_quartz' if (x//4+z//4)%2 else 'minecraft:polished_diorite')
        if x%8 in (0,1):
            s.fill(x,1,5,x,5,5,'minecraft:polished_diorite')
            s.fill(x,1,14,x,5,14,'minecraft:polished_diorite')
        if x%8==4:
            s.put(x,5,5,'minecraft:sea_lantern')
            s.put(x,5,14,'minecraft:sea_lantern')
    for x in range(5,48,8):
        s.fill(x,1,3,x+3,4,3,'minecraft:iron_block')
        s.fill(x+1,1,3,x+2,3,3,'minecraft:white_concrete')
        s.fill(x,1,16,x+3,4,16,'minecraft:iron_block')
        s.fill(x+1,1,16,x+2,3,16,'minecraft:white_concrete')
    s.fill(48,1,7,51,5,12,'minecraft:red_concrete')
    s.fill(49,1,8,50,4,11,'minecraft:iron_block')

def detailed_office(s):
    for x in range(1,39):
        for z in range(1,24):
            if (x//5+z//5)%2:s.put(x,0,z,'minecraft:polished_blackstone')
        if x%7==0:
            s.fill(x,1,0,x+1,5,0,'minecraft:dark_oak_planks')
            s.fill(x,1,24,x+1,5,24,'minecraft:dark_oak_planks')
            s.fill(x,6,2,x+2,6,3,'minecraft:sea_lantern')
    for x,z in ((4,6),(15,6),(25,6),(4,18),(17,18)):
        s.fill(x,1,z,x+5,1,z+2,'minecraft:dark_oak_planks')
        s.fill(x,2,z+2,x+5,2,z+2,'minecraft:cut_copper')
        s.put(x+1,2,z+1,'minecraft:lantern')
        s.fill(x+3,1,z+4,x+4,1,z+4,'minecraft:dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
    s.fill(3,1,17,13,3,20,'minecraft:polished_deepslate')
    s.fill(5,4,18,11,4,18,'minecraft:iron_block')
    for x in (6,8,10):s.put(x,4,17,'minecraft:stone_button[face=wall,facing=north,powered=false]')

def lobby():
    s=Set('01_lobby_and_lift',46,13,38)
    s.box(1,0,3,44,11,36,'minecraft:polished_deepslate','minecraft:polished_diorite','minecraft:smooth_quartz')
    s.fill(2,1,4,43,9,35,'minecraft:air')
    # Windowed street frontage and vestibule.
    for x in range(2,44):
        for y in range(1,10):s.put(x,y,3,'minecraft:tinted_glass' if x<19 or x>26 else 'minecraft:air')
    s.fill(19,1,3,26,4,3,'minecraft:air')
    for x in (1,12,32,44):s.fill(x,1,3,x+1,9,3,'minecraft:polished_deepslate')
    # Reception desk, lamps, lounges, patterned lobby floor.
    s.fill(6,1,19,19,2,21,'minecraft:dark_oak_planks')
    s.fill(7,3,20,18,3,20,'minecraft:polished_blackstone_slab[type=bottom,waterlogged=false]')
    for x,z in [(7,15),(17,15),(6,29),(15,29),(30,12),(40,12)]:
        s.fill(x,1,z,x+2,2,z+2,'minecraft:spruce_planks')
        s.put(x+1,3,z+1,'minecraft:lantern[hanging=false,waterlogged=false]')
    for z in (8,16,24,32):
        s.fill(22,0,z,25,0,z+1,'minecraft:polished_blackstone')
    lights(s,10,[(x,z) for x in (5,15,27,39) for z in (8,18,29)])
    luxury_lobby(s)
    # Lift on north wall, with actual 7x7 chamber to hide transitions.
    lift(s,33,28,1)
    s.fill(32,1,27,40,5,27,'minecraft:iron_block')
    s.fill(34,1,27,38,4,27,'minecraft:air')
    s.put(39,2,27,'minecraft:stone_button[face=wall,facing=south,powered=false]')
    # Street apron, exterior lamps and two planters.
    s.fill(1,0,0,44,0,2,'minecraft:stone_bricks')
    for x in (5,41):
        s.fill(x,1,5,x+2,2,7,'minecraft:polished_blackstone')
        s.put(x+1,3,6,'minecraft:oak_leaves[persistent=true,distance=1]')
    s.export()

def office():
    s=Set('02_office_and_fuse_panel',40,8,25)
    s.box(0,0,0,39,7,24,'minecraft:light_gray_concrete','minecraft:polished_andesite','minecraft:smooth_quartz')
    s.fill(1,1,1,38,6,23,'minecraft:air')
    for x in (8,18,28):
        s.fill(x,1,4,x+1,3,9,'minecraft:dark_oak_planks')
        s.fill(x+3,1,5,x+5,1,8,'minecraft:black_carpet')
        s.put(x+1,4,4,'minecraft:lantern[hanging=false,waterlogged=false]')
    s.fill(3,1,17,13,3,20,'minecraft:polished_deepslate')
    s.fill(5,4,18,11,4,18,'minecraft:iron_block')
    for x in (6,8,10):s.put(x,4,17,'minecraft:stone_button[face=wall,facing=north,powered=false]')
    lights(s,6,[(x,z) for x in (5,15,25,35) for z in (3,13,20)])
    detailed_office(s)
    lift(s,32,17,1);s.door(33,1,17,'x',5,4)
    s.export()

def hotel():
    s=Set('03_impossible_hotel_13',52,8,20)
    s.box(0,0,0,51,7,19,'minecraft:stone_bricks','minecraft:dark_oak_planks','minecraft:spruce_planks')
    s.fill(1,1,1,50,6,18,'minecraft:air')
    for x in range(2,50):s.fill(x,0,8,x,0,11,'minecraft:red_carpet')
    for x in (7,16,25,34,43):
        for z1,z2 in ((1,6),(13,18)):
            s.fill(x,1,z1,x,6,z2,'minecraft:dark_oak_planks')
            s.fill(x+3,1,z1+2,x+5,1,z1+4,'minecraft:brown_carpet')
            s.fill(x+4,1,z1+4,x+5,2,z1+5,'minecraft:spruce_planks')
            s.door(x+2,1,7 if z1==1 else 12,'x',2,3)
        s.fill(x,1,7,x,5,7,'minecraft:dark_oak_planks')
        s.fill(x,1,12,x,5,12,'minecraft:dark_oak_planks')
    for x in (3,12,21,30,39,48):
        s.put(x,3,7,'minecraft:lantern[hanging=false,waterlogged=false]')
        s.put(x,3,12,'minecraft:lantern[hanging=false,waterlogged=false]')
    lights(s,6,[(x,9) for x in (5,17,29,41)])
    detailed_hotel(s)
    lift(s,1,7,1);s.door(7,1,8,'z',4,4)
    s.export()

def maintenance():
    s=Set('04_maintenance_chase',52,8,20)
    s.box(0,0,0,51,7,19,'minecraft:gray_concrete','minecraft:deepslate_tiles','minecraft:deepslate_tiles')
    s.fill(1,1,1,50,6,18,'minecraft:air')
    s.fill(0,1,6,51,5,6,'minecraft:iron_block')
    s.fill(0,1,13,51,5,13,'minecraft:iron_block')
    for x in range(4,51,7):
        for z in (2,15):
            s.fill(x,1,z,x+2,3,z+2,'minecraft:iron_block')
            s.put(x+1,2,z+1,'minecraft:stone_button[face=wall,facing=south,powered=false]')
        s.fill(x,5,7,x+3,5,7,'minecraft:oxidized_copper')
        s.fill(x,5,12,x+3,5,12,'minecraft:oxidized_copper')
    for x in (6,18,30,42):
        s.fill(x,6,8,x+1,6,9,'minecraft:redstone_lamp[lit=true]')
        s.fill(x,6,11,x+1,6,12,'minecraft:redstone_lamp[lit=true]')
    s.fill(40,1,2,50,5,5,'minecraft:gray_concrete')
    s.fill(41,1,3,49,4,4,'minecraft:air')
    s.door(43,1,6,'x',3,4)
    detailed_maintenance(s)
    lift(s,1,7,1);s.door(7,1,8,'z',4,4)
    s.export()

def zero():
    s=Set('05_floor_zero_and_exit',52,8,20)
    s.box(0,0,0,51,7,19,'minecraft:smooth_quartz','minecraft:polished_diorite','minecraft:smooth_quartz')
    s.fill(1,1,1,50,6,18,'minecraft:air')
    s.fill(0,1,5,51,5,5,'minecraft:smooth_quartz')
    s.fill(0,1,14,51,5,14,'minecraft:smooth_quartz')
    for x in range(5,48,8):
        for z in (4,15):
            s.fill(x,1,z,x+3,4,z,'minecraft:iron_block')
            s.fill(x+1,1,z,x+2,3,z,'minecraft:smooth_quartz')
        s.fill(x,6,9,x+2,6,10,'minecraft:sea_lantern')
    s.fill(48,1,7,51,5,12,'minecraft:red_concrete')
    s.fill(49,1,8,50,4,11,'minecraft:iron_block')
    s.fill(3,1,6,7,4,6,'minecraft:air')
    detailed_zero(s)
    lift(s,1,7,1)
    s.export()

def stair():
    s=Set('06_looping_stairwell',18,25,18)
    s.box(0,0,0,17,24,17,'minecraft:gray_concrete','minecraft:polished_andesite','minecraft:gray_concrete')
    s.fill(1,1,1,16,23,16,'minecraft:air')
    for level in (0,8,16):
        s.fill(1,level,1,16,level,16,'minecraft:polished_andesite')
        for n in range(1,8):
            s.fill(3+n,level+n,3,7+n,level+n,6,'minecraft:stone_bricks')
        s.fill(3,level+1,14,7,level+4,14,'minecraft:iron_block')
        s.door(4,level+1,14,'x',3,4)
        s.put(10,level+5,14,'minecraft:redstone_lamp[lit=true]')
    s.export()

if __name__=='__main__':
    lobby();office();hotel();maintenance();zero();stair()
