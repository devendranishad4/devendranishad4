"""Block-for-block Minecraft 1.20.1 map draft: Room 203 residential street.

Exports a Sponge v2 WorldEdit schematic. Coordinates in this file are local
schematic coordinates; the camera preview is rendered from the same palette.
"""
from pathlib import Path
import sys
import os

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'LastElevatorBuild'))
import build_sets

OUT = Path(__file__).resolve().parent
build_sets.OUT = OUT
MODDED=os.environ.get('ROOM203_MACAW','0')=='1'
S = build_sets.Set('room203_town_v2_macaw' if MODDED else 'room203_town_v2', 256, 29, 224)

def put(x,y,z,b): S.put(x,y,z,'minecraft:'+b)
def fill(x1,y1,z1,x2,y2,z2,b): S.fill(x1,y1,z1,x2,y2,z2,'minecraft:'+b)

AIR='air'; ASPHALT='gray_concrete'; EDGE='polished_andesite'; WALL='light_gray_concrete'
WORN='tuff_bricks'; TRIM='polished_deepslate'; GLASS='gray_stained_glass_pane'

# Continuous ground, main one-car-width street, and a crossing side street.
fill(0,0,0,255,0,223,'stone')
fill(38,0,0,50,0,223,ASPHALT)
fill(137,0,0,151,0,223,ASPHALT)
for z0,z1 in ((13,25),(82,94),(150,162)):
    fill(0,0,z0,255,0,z1,ASPHALT)
for x in (36,37,51,52,135,136,152,153):fill(x,0,0,x,0,223,EDGE)
for z in (11,12,26,27,80,81,95,96,148,149,163,164):fill(0,0,z,255,0,z,EDGE)
for z in range(4,220,12):
    put(44,0,z,'light_gray_concrete')
    put(44,0,z+1,'light_gray_concrete')
for x in range(40,50,2):fill(x,0,24,x,0,25,'white_concrete')
for x in range(138,151,2):
    fill(x,0,94,x,0,96,'white_concrete')
    fill(x,0,162,x,0,164,'white_concrete')
for z in range(4,220,12):put(144,0,z,'light_gray_concrete')
for z in (31,57,89,119,179,209):
    fill(38,0,z,38,0,z+2,'polished_blackstone')
    fill(50,0,z+3,50,0,z+5,'polished_blackstone')
for x,z in ((42,40),(46,42),(43,67),(47,69),(40,84),(45,102)):
    fill(x,0,z,x+1,0,z+2,'water[level=0]')
    put(x+2,0,z+1,'polished_blackstone')
for x,z in ((140,34),(147,67),(142,108),(149,136),(141,181),(146,205),
            (43,135),(48,176),(42,204)):
    fill(x,0,z,x+2,0,z+1,'water[level=0]')

# Facade of the protagonist's four-floor apartment, with open galleries.
# Main building x=59..84, z=27..77. Front gallery x=55..59.
fill(59,0,27,84,0,77,'polished_deepslate')
for f in range(4):
    base=f*6
    fill(55,base,27,84,base,77,'smooth_stone')
    fill(55,base,27,59,base,77,'polished_andesite')
    fill(59,base+1,27,84,base+4,27,WORN)
    fill(59,base+1,77,84,base+4,77,WORN)
    fill(84,base+1,27,84,base+4,77,WALL)
    fill(59,base+5,27,84,base+5,77,'deepslate_tiles')
    # Six independent apartments on each floor; all have actual partitions.
    for j in range(7):
        z=28+j*7
        fill(59,base+1,z,84,base+4,z,WORN)
        fill(58,base+1,z,58,base+4,z,TRIM)
    for j in range(6):
        z=30+j*7
        # Front recess, physically passable entrance; no fake doors.
        fill(59,base+1,z,59,base+2,z+1,AIR)
        fill(60,base+1,z,60,base+2,z+1,AIR)
        fill(59,base+1,z+1,59,base+2,z+1,WORN)
        put(59,base+1,z,'spruce_door[facing=west,half=lower,hinge=left,open=false,powered=false]')
        put(59,base+2,z,'spruce_door[facing=west,half=upper,hinge=left,open=false,powered=false]')
        put(60,base+4,z,'ochre_froglight' if f==1 and j==2 else 'redstone_lamp[lit=false]')
        fill(84,base+2,z,84,base+3,z+2,'gray_stained_glass')
        # A compact genkan, kitchen and sleeping area with a real internal route.
        fill(71,base+1,z-2,71,base+3,z-1,'stripped_spruce_log')
        fill(71,base+1,z+1,71,base+3,z+2,'stripped_spruce_log')
        fill(72,base+1,z-2,75,base+1,z-2,'dark_oak_planks')
        put(73,base+2,z-2,'barrel')
        fill(78,base+1,z-2,80,base+1,z-2,'dark_oak_planks')
        put(79,base+2,z-2,'barrel')
        fill(65,base+1,z+2,67,base+1,z+2,'spruce_slab[type=bottom]')
        put(66,base+1,z+1,'dark_oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]')
        # Every unit has a real bedroom, cabinet, kitchen and sitting nook.
        if not (f==1 and j==2):
            put(79,base+1,z+1,'white_bed[facing=south,part=foot,occupied=false]')
            put(79,base+1,z+2,'white_bed[facing=south,part=head,occupied=false]')
        fill(81,base+1,z+1,82,base+2,z+1,'barrel')
        fill(68,base+2,z+2,69,base+3,z+2,'black_concrete')
        fill(68,base+1,z+2,69,base+1,z+2,'polished_deepslate_slab[type=bottom,waterlogged=false]')
        put(74,base+1,z-2,'water_cauldron[level=3]')
        fill(82,base+2,z+2,83,base+3,z+2,'gray_stained_glass_pane')
    for z in range(28,78,7):
        fill(55,base+1,z,55,base+3,z,'polished_deepslate_wall')
        fill(55,base+1,z+1,55,base+1,z+6,'iron_bars')
    # Selectively lit galleries: dark gaps help the horror silhouette read.
    for z in (31,45,66):put(57,base+4,z,'sea_lantern')
    fill(59,base+1,28,59,base+4,28,'polished_deepslate')
    fill(59,base+1,76,59,base+4,76,'polished_deepslate')

# Room 203 (second floor, third unit) is furnished and warm. Room 204 dark.
fill(61,6,44,83,6,48,'spruce_planks')
fill(59,8,46,59,9,47,'orange_stained_glass')
put(61,9,46,'shroomlight')
put(79,7,47,'white_bed[facing=south,part=foot,occupied=false]')
put(79,7,48,'white_bed[facing=south,part=head,occupied=false]')
fill(77,7,47,78,7,48,'white_wool')
fill(81,7,45,82,7,46,'dark_oak_planks')
put(82,8,46,'lantern')
fill(62,7,46,62,8,47,'bookshelf')
put(83,10,46,'ochre_froglight')
fill(74,7,48,76,7,48,'light_gray_carpet')
put(75,7,47,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
put(82,7,48,'flower_pot')
# The adjacent 204 stays visually empty and unlit, with a view to the street.
fill(61,7,51,83,10,55,AIR)
fill(59,8,53,59,9,54,'tinted_glass')
fill(84,8,52,84,9,54,'tinted_glass')

# Physical stair: broad landings and six solid steps between each floor.
# It remains open to the street; floors are reached without teleportation.
for f in range(3):
    y=f*6
    zbase=29+f*13
    fill(51,y,zbase,57,y,zbase+10,'polished_deepslate')
    for i in range(1,7):
        fill(52,y+i,zbase+1+i,54,y+i,zbase+1+i,'polished_andesite')
        if i<6:fill(51,y+i,zbase+1+i,51,y+i+1,zbase+1+i,'iron_bars')
    fill(52,y+6,zbase+7,59,y+6,zbase+10,'polished_andesite')
    fill(50,y+1,zbase,50,y+6,zbase,'polished_deepslate')
    fill(50,y+1,zbase+10,50,y+6,zbase+10,'polished_deepslate')
fill(59,24,27,84,24,77,'deepslate_tile_slab[type=bottom]')
for z in range(28,77,8):
    fill(59,25,z,84,25,z,'polished_deepslate')
for x in (59,84):fill(x,25,27,x,26,77,'iron_bars')

# Entrance court, mailboxes, covered bicycles and service lane behind.
fill(52,0,42,54,0,68,'polished_andesite')
fill(51,1,46,51,3,49,'iron_block')
for z in (46,47,48,49):put(51,2,z,'stone_button[face=wall,facing=west,powered=false]')
put(58,8,44,'oak_wall_sign[facing=west,waterlogged=false]')
put(58,8,51,'oak_wall_sign[facing=west,waterlogged=false]')
fill(52,1,65,55,1,69,'dark_oak_slab[type=bottom]')
for z in (66,68):
    put(53,1,z,'black_concrete');put(54,1,z,'iron_bars')
fill(86,0,27,91,0,77,'gravel')
fill(88,1,31,91,2,35,'barrel')
for z in (34,54,72):fill(85,1,z,85,3,z,'stone_bricks')

def shop(x0,z0,width,length,height,wall):
    x1=x0+width-1;z1=z0+length-1
    fill(x0,0,z0,x1,0,z1,'polished_andesite')
    for y0 in range(0,height*5,5):
        fill(x0,y0+1,z0,x1,y0+4,z0,wall)
        fill(x0,y0+1,z1,x1,y0+4,z1,wall)
        fill(x0,y0+1,z0,x0,y0+4,z1,wall)
        fill(x1,y0+1,z0,x1,y0+4,z1,wall)
        fill(x0,y0+5,z0,x1,y0+5,z1,'deepslate_tiles')
        fill(x1,y0+2,z0+3,x1,y0+3,z1-3,'light_blue_stained_glass')
    for x in range(x0+2,x1-1,5):
        fill(x,2,z1,x+2,3,z1,'gray_stained_glass')
        if height>1:fill(x,7,z1,x+2,8,z1,'gray_stained_glass')
    fill(x0+width//2,1,z1,x0+width//2+1,2,z1,AIR)
    fill(x0-1,height*5+1,z0-1,x1+1,height*5+1,z1+1,'deepslate_tile_slab[type=bottom]')
    # Occupied ground floors: window seats, kitchen counters and ceiling light.
    fill(x0+3,1,z0+3,x0+7,1,z0+3,'dark_oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]')
    fill(x0+4,1,z0+5,x0+6,1,z0+5,'spruce_slab[type=bottom]')
    fill(x1-6,1,z0+3,x1-2,1,z0+4,'smooth_quartz')
    put(x1-4,2,z0+3,'water_cauldron[level=3]')
    fill(x0+width//2-1,4,z0+length//2,x0+width//2+1,4,z0+length//2,'ochre_froglight')
    for floor in range(1,height):
        base=floor*5
        fill(x0+3,base+1,z0+4,x0+5,base+1,z0+5,'white_wool')
        fill(x1-5,base+1,z1-5,x1-3,base+1,z1-5,'spruce_slab[type=bottom]')
        put(x0+width//2,base+4,z0+length//2,'ochre_froglight')
    # Roof plant and service hardware; alternating frontage avoids cloned boxes.
    for xx in range(x0+2,x1-1,7):
        fill(xx,height*5+2,z0+3,xx+2,height*5+3,z0+5,'polished_deepslate')
        put(xx+1,height*5+4,z0+4,'iron_bars')
    accent=('cut_copper','dark_oak_planks','polished_blackstone_bricks',
            'mossy_stone_bricks')[(x0+z0)%4]
    fill(x0+1,4,z1+1,x1-1,4,z1+1,accent)
    for xx in range(x0+2,x1-1,6):
        fill(xx,1,z1+1,xx+2,1,z1+1,'polished_andesite')
        if height>1:
            fill(xx,6,z1+1,xx+2,6,z1+1,'iron_bars')
            put(xx+1,7,z1+1,'polished_andesite_slab[type=bottom,waterlogged=false]')
    # A lit entry and one room window, with most units kept dark.
    put(x0+width//2,3,z1+1,'lantern[hanging=true,waterlogged=false]')
    if height>1 and (x0+z0)%3==0:
        fill(x1,7,z0+4,x1,8,z0+6,'orange_stained_glass')
        put(x1-1,8,z0+5,'shroomlight')
    for yy in range(2,height*5,5):
        put(x0,yy,z1-3,'mossy_stone_bricks')

# A real neighbourhood, not one facade in an empty void.
shop(5,28,29,19,3,'tuff_bricks')
shop(8,54,25,17,2,'mossy_stone_bricks')
shop(3,79,32,22,3,'light_gray_concrete')
shop(62,98,29,25,2,'gray_concrete')
shop(60,2,32,10,2,'stone_bricks')
shop(5,2,29,10,2,'tuff_bricks')
# The far road ends at a believable T junction, rather than a visible void.
shop(37,0,20,10,2,'mossy_stone_bricks')
for args in (
    (4,102,29,34,3,'tuff_bricks'),(5,166,28,27,2,'light_gray_concrete'),
    (8,198,26,20,2,'mossy_stone_bricks'),
    (62,125,30,18,3,'stone_bricks'),(61,166,31,27,3,'tuff_bricks'),
    (65,198,28,20,2,'gray_concrete'),
    (104,29,27,24,3,'light_gray_concrete'),(105,58,25,20,2,'tuff_bricks'),
    (106,99,26,30,3,'stone_bricks'),(105,166,28,27,2,'mossy_stone_bricks'),
    (105,198,27,19,2,'gray_concrete'),
    (158,29,31,23,3,'tuff_bricks'),(159,58,31,20,2,'light_gray_concrete'),
    (157,100,34,30,4,'stone_bricks'),(159,166,32,27,3,'gray_concrete'),
    (160,198,30,18,2,'mossy_stone_bricks'),
    (199,29,37,31,3,'light_gray_concrete'),(198,99,39,31,3,'tuff_bricks'),
    (201,166,36,30,3,'gray_concrete'),(200,199,36,18,2,'stone_bricks')):
    shop(*args)
for x,y,z in ((33,7,33),(33,7,60),(34,7,88),(60,7,87)):
    put(x,y,z,'orange_stained_glass')
    if x==33:put(x-1,y,z,'shroomlight')
    elif x==34:put(x-1,y,z,'shroomlight')
    else:put(x+1,y,z,'shroomlight')

# Downstairs shuttered shop, lit vending machine and quiet side alleys.
fill(8,1,46,29,4,46,'iron_block')
for x in range(9,29,3):fill(x,1,46,x,4,46,'polished_deepslate')
fill(33,1,45,35,4,47,'blue_concrete')
fill(33,2,47,35,3,47,'sea_lantern')
fill(33,1,47,35,1,47,'white_concrete')
for x in (6,22):
    fill(x,1,51,x+4,1,51,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
fill(54,0,78,58,0,106,'polished_andesite')
for z in (88,96):fill(55,1,z,55,2,z,'barrel')

# Utility poles, block-state chain wires, uneven exterior light.
for x,z in ((36,30),(36,63),(36,95),(36,128),(36,177),(36,211),
            (51,9),(51,82),(51,131),(51,193),
            (135,30),(135,71),(135,118),(135,181),(135,211),
            (153,43),(153,111),(153,188)):
    fill(x,1,z,x,22,z,'stripped_dark_oak_log')
    fill(x-2,19,z,x+2,19,z,'dark_oak_fence')
    fill(x-1,17,z,x+1,18,z,'polished_deepslate')
    put(x+2,18,z,'sea_lantern')
for x,z0,z1 in ((36,30,63),(36,63,95),(36,95,128),
                (36,128,177),(36,177,211),(51,9,82),(51,82,131),
                (51,131,193),(135,30,71),(135,71,118),
                (135,118,181),(135,181,211),(153,43,111),(153,111,188)):
    for zz in range(z0+1,z1):put(x,20,zz,'chain[axis=z]')
for z in (30,63,95,128,177,211):
    for x in range(37,51):put(x,21,z,'chain[axis=x]')
for z in (43,111,188):
    for x in range(136,153):put(x,21,z,'chain[axis=x]')
for x,z in ((35,39),(35,73),(52,53),(54,91)):
    fill(x,1,z,x,2,z,'oak_leaves[persistent=true,distance=1]')
for x,z in ((34,82),(54,71),(52,27)):
    put(x,0,z,'mossy_cobblestone')
for x,z in ((34,116),(35,181),(54,113),(54,204),
            (133,46),(155,105),(191,178),(238,180)):
    fill(x,1,z,x+1,2,z+1,'barrel')
    put(x,3,z,'dark_oak_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]')
for x,z in ((34,136),(101,107),(132,175),(155,30),(197,110)):
    fill(x,1,z,x+2,3,z,'blue_concrete')
    fill(x,2,z,x+2,2,z,'sea_lantern')
    fill(x,1,z,x+2,1,z,'white_concrete')
# Two parked cars and a bike row; neither blocks the full width of a street.
for x,z,body in ((148,43,'white_concrete'),(48,119,'black_concrete')):
    fill(x,1,z,x+2,1,z+5,body)
    fill(x,2,z+2,x+2,2,z+4,'light_blue_stained_glass')
    for zz in (z+1,z+4):
        put(x-1,1,zz,'black_concrete');put(x+3,1,zz,'black_concrete')
    put(x,1,z,'sea_lantern');put(x+2,1,z,'sea_lantern')
for x,z in ((53,173),(53,175),(134,39),(154,178)):
    put(x,1,z,'black_concrete')
    put(x,2,z,'iron_bars')
# Rusted AC units, drainpipes, and staggered windows on the street facades.
for z in (31,47,63,74):
    for y in (3,9,15,21):
        fill(61,y,z,62,y,z,'iron_trapdoor[facing=north,half=bottom,open=false,powered=false,waterlogged=false]')
for x,z in ((10,32),(16,34),(26,55),(9,86),(29,82)):
    fill(x,2,z,x,4,z,'iron_bars')
    put(x,5,z,'polished_deepslate_slab[type=bottom,waterlogged=false]')

# Verified against Macaw's Furniture 3.4.1 Forge 1.20.1 JAR. Keep the vanilla
# world separately so the optional furniture requirement cannot corrupt it.
if MODDED:
    IDs=set((OUT/'furniture-blocks.txt').read_text().splitlines())
    def furniture(x,y,z,name):
        assert name in IDs,name
        S.put(x,y,z,'mcwfurnitures:'+name)
    for f in range(4):
        y=f*6+1
        for j in range(6):
            z=30+j*7
            furniture(66,y,z+1,'dark_oak_modern_chair')
            furniture(65,y,z+2,'dark_oak_coffee_table')
            furniture(73,y,z-2,'dark_oak_kitchen_cabinet')
            put(73,y+1,z-2,AIR)
            furniture(74,y,z-2,'dark_oak_kitchen_sink')
            furniture(81,y,z+1,'dark_oak_modern_wardrobe')
            put(81,y+1,z+1,AIR)
            furniture(68,y,z+1,'dark_oak_modern_desk')
    furniture(75,7,48,'gray_couch')
    furniture(81,7,45,'dark_oak_modern_desk')
    for x,z in ((54,67),(53,69),(134,101),(155,101)):
        furniture(x,1,z,'dark_oak_modern_chair')

S.export()
print('Player arrival: 44,1,92; Room 203 second floor: 65,7,45')
print(f'Town footprint: {S.w} x {S.d}; physical build: {S.h} blocks high')
