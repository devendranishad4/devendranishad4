"""Room 203 residential district, with detailed streets and furnished buildings.

Exports a Sponge v2 WorldEdit schematic. Coordinates in this file are local
schematic coordinates; the camera preview is rendered from the same palette.
"""
from pathlib import Path
import sys
import os

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_sets

OUT = Path(__file__).resolve().parent
build_sets.OUT = OUT
MODDED=os.environ.get('ROOM203_MACAW','0')=='1'
S = build_sets.Set('room203_district_v3_macaw' if MODDED else 'room203_district_v3', 480, 29, 344)

def put(x,y,z,b): S.put(x,y,z,'minecraft:'+b)
def fill(x1,y1,z1,x2,y2,z2,b): S.fill(x1,y1,z1,x2,y2,z2,'minecraft:'+b)

AIR='air'; ASPHALT='gray_concrete'; EDGE='polished_andesite'; WALL='light_gray_concrete'
WORN='tuff_bricks'; TRIM='polished_deepslate'; GLASS='gray_stained_glass_pane'

# Continuous ground, main one-car-width street, and a crossing side street.
fill(0,0,0,479,0,343,'stone')
fill(38,0,0,50,0,223,ASPHALT)
fill(137,0,0,151,0,223,ASPHALT)
for z0,z1 in ((13,25),(82,94),(150,162),(232,248),(314,330)):
    fill(0,0,z0,479,0,z1,ASPHALT)
for x in (36,37,51,52,135,136,152,153,233,234,252,253,333,334,352,353,433,434,452,453):fill(x,0,0,x,0,343,EDGE)
for z in (11,12,26,27,80,81,95,96,148,149,163,164,230,231,249,250,312,313,331,332):fill(0,0,z,479,0,z,EDGE)
for z in range(4,340,12):
    put(44,0,z,'light_gray_concrete')
    put(44,0,z+1,'light_gray_concrete')
for x in range(40,50,2):fill(x,0,24,x,0,25,'white_concrete')
for x in range(138,151,2):
    fill(x,0,94,x,0,96,'white_concrete')
    fill(x,0,162,x,0,164,'white_concrete')
for z in range(4,340,12):put(144,0,z,'light_gray_concrete')
for z in (31,57,89,119,179,209,273,303):
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

# Exterior access: real stair-shaped treads with slim supports and a continuous
# 3-block-wide flight. Each landing joins the open gallery at the next level.
for f in range(3):
    y=f*6
    zbase=29+f*13
    fill(52,y,zbase,59,y,zbase+2,'polished_andesite')
    for i in range(6):
        zz=zbase+3+i
        fill(52,y+i,zz,54,y+i,zz,
             'stone_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]')
        if i in (2,5):
            fill(52,y,zz,52,y+i-1,zz,'polished_deepslate_wall')
            fill(54,y,zz,54,y+i-1,zz,'polished_deepslate_wall')
        put(51,y+i+1,zz,'iron_bars')
        put(55,y+i+1,zz,'iron_bars')
        if i in (0,3,5):
            put(51,y+i+2,zz,'iron_bars')
            put(55,y+i+2,zz,'iron_bars')
    fill(52,y+6,zbase+9,59,y+6,zbase+11,'polished_andesite')
    fill(51,y+7,zbase+9,51,y+7,zbase+11,'iron_bars')
    for zz in (zbase+9,zbase+11):
        fill(52,y,zz,52,y+5,zz,'stripped_dark_oak_log')
    put(52,y+8,zbase+10,'lantern[hanging=true,waterlogged=false]')
# A finished low roof: overhanging eaves, parapet and a few rooftop services.
fill(54,24,25,87,24,79,'deepslate_tile_slab[type=bottom]')
fill(59,25,27,84,25,27,'polished_deepslate')
fill(59,25,77,84,25,77,'polished_deepslate')
for x in (59,84):fill(x,25,27,x,25,77,'polished_deepslate')
for z in (36,54,69):
    fill(77,25,z,81,26,z+2,'polished_deepslate')
    put(79,27,z+1,'iron_bars')

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
        for zz in range(z0+4,z1-3,7):
            fill(x0,y0+2,zz,x0,y0+3,zz+2,'gray_stained_glass')
            fill(x0-1,y0+1,zz,x0-1,y0+1,zz+2,'dark_oak_planks')
            fill(x0-1,y0+4,zz,x0-1,y0+4,zz+2,'cut_copper')
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

# Complete the outer two columns and the southern row. The old district is
# retained around Room 203, while these blocks create a connected filming area.
for x0,x1 in ((38,50),(137,151),(233,252),(333,352),(433,452)):
    fill(x0,0,0,x1,0,343,ASPHALT)
for z0,z1 in ((13,25),(82,94),(150,162),(232,248),(314,330)):
    fill(0,0,z0,479,0,z1,ASPHALT)
for x in (36,37,51,52,135,136,152,153,231,232,253,254,
          331,332,353,354,431,432,453,454):
    fill(x,0,0,x,0,343,EDGE)
for z in (11,12,26,27,80,81,95,96,148,149,163,164,
          230,231,249,250,312,313,331,332):
    fill(0,0,z,479,0,z,EDGE)
for x in (44,144,243,343,443):
    for z in range(33,313,11):
        if not any(a<=z<=b for a,b in ((82,94),(150,162),(232,248))):
            fill(x,0,z,x,0,z+2,'light_gray_concrete')

def residential_shop(x,z,w,d,floors,variant):
    """Accessible ground shop and upper apartments, with distinct street faces."""
    x1,z1=x+w-1,z+d-1
    facade=('tuff_bricks','light_gray_concrete','mossy_stone_bricks',
            'polished_andesite','stone_bricks','mud_bricks')[variant%6]
    accent=('dark_oak_planks','oxidized_copper','cut_copper',
            'red_terracotta','polished_blackstone_bricks','spruce_planks')[variant%6]
    tile=('deepslate_tiles','dark_prismarine','deepslate_bricks')[variant%3]
    fill(x,0,z,x1,0,z1,'polished_andesite')
    for floor in range(floors):
        y=floor*5
        fill(x,y+1,z,x1,y+4,z,facade)
        fill(x,y+1,z1,x1,y+4,z1,facade)
        fill(x,y+1,z,x,y+4,z1,facade)
        fill(x1,y+1,z,x1,y+4,z1,facade)
        fill(x,y+5,z,x1,y+5,z1,'spruce_planks')
        # Paired front windows, side windows, and strong vertical pilasters.
        for left in (x+3,x+w//2+2):
            right=min(left+4,x1-2)
            fill(left,y+2,z,right,y+3,z,'gray_stained_glass')
            fill(left,y+1,z-1,right,y+1,z-1,'dark_oak_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]')
            fill(left,y+4,z-1,right,y+4,z-1,accent)
            fill(left,y+2,z1,right,y+3,z1,'gray_stained_glass')
        for xx in (x+1,x+w//2,x1-1):
            fill(xx,y+1,z-1,xx,y+4,z-1,accent)
        for zz in range(z+4,z1-2,7):
            fill(x1,y+2,zz,x1,y+3,zz+2,'gray_stained_glass')
            fill(x1+1,y+1,zz,x1+1,y+1,zz+2,accent)
            fill(x,y+2,zz,x,y+3,zz+2,'gray_stained_glass')
            fill(x-1,y+1,zz,x-1,y+1,zz+2,accent)
            fill(x-1,y+4,zz,x-1,y+4,zz+2,tile)
        if floor and floor%2:
            fill(x-2,y,z+4,x-1,y,z1-4,'polished_andesite')
            for zz in range(z+4,z1-3,4):
                put(x-2,y+1,zz,'iron_bars')
        # Enclosed upper flat: bedroom, living seat, kitchen and light.
        if floor:
            fill(x+2,y+1,z+2,x+6,y+1,z+3,'white_wool')
            fill(x+7,y+1,z+2,x+7,y+2,z+3,'barrel')
            fill(x+3,y+1,z1-5,x+7,y+1,z1-5,'dark_oak_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]')
            fill(x+4,y+1,z1-7,x+6,y+1,z1-7,'spruce_slab[type=bottom]')
            fill(x1-7,y+1,z1-4,x1-3,y+1,z1-4,'smooth_quartz')
            put(x1-5,y+2,z1-4,'water_cauldron[level=3]')
            put(x+w//2,y+4,z+d//2,'ochre_froglight')
            if MODDED:
                furniture(x+10,y+1,z+6,'dark_oak_modern_chair')
                furniture(x+11,y+1,z+6,'dark_oak_coffee_table')
                furniture(x1-8,y+1,z1-5,'dark_oak_kitchen_cabinet')
                furniture(x1-7,y+1,z1-5,'dark_oak_kitchen_sink')
                furniture(x+8,y+1,z+2,'dark_oak_modern_wardrobe')
        else:
            # Shop shelving, counter and a central aisle from the street door.
            fill(x+3,1,z+4,x+3,3,z1-5,'barrel')
            fill(x1-7,1,z1-5,x1-3,1,z1-5,'dark_oak_planks')
            put(x1-5,2,z1-5,'water_cauldron[level=3]')
            fill(x+8,1,z+8,x+10,1,z+8,'spruce_slab[type=bottom]')
            put(x+w//2,4,z+d//2,'ochre_froglight')
            if MODDED:
                furniture(x1-8,1,z1-7,'dark_oak_modern_desk')
                furniture(x1-9,1,z1-7,'dark_oak_modern_chair')
        # Walkable galleries and balconies give the facade physical depth.
        if floor and floor%2:
            fill(x+2,y,z-2,x1-2,y,z-1,'polished_andesite')
            for xx in range(x+2,x1-1,3):
                put(xx,y+1,z-2,'iron_bars')
        fill(x,y+5,z,x1,y+5,z,accent)
    # Main street entrance with a real two-block door.
    cx=x+w//2
    fill(cx,1,z,cx,2,z,AIR)
    put(cx,1,z,'spruce_door[facing=north,half=lower,hinge=left,open=false,powered=false]')
    put(cx,2,z,'spruce_door[facing=north,half=upper,hinge=left,open=false,powered=false]')
    fill(cx-3,3,z-2,cx+3,3,z-1,accent)
    put(cx,3,z-2,'sea_lantern')
    # Layered roof, parapet, aerial and service ducts.
    ry=floors*5
    fill(x,ry+1,z,x1,ry+1,z,tile)
    fill(x,ry+1,z1,x1,ry+1,z1,tile)
    fill(x,ry+1,z,x,ry+1,z1,tile)
    fill(x1,ry+1,z,x1,ry+1,z1,tile)
    for xx in range(x+3,x1-3,8):
        fill(xx,ry+1,z1-6,xx+3,ry+2,z1-4,'polished_deepslate')
        put(xx+1,ry+3,z1-5,'iron_bars')
    for zz in range(z+5,z1-4,8):
        fill(x1+1,2,zz,x1+1,3,zz+1,'iron_trapdoor[facing=east,half=bottom,open=false,powered=false,waterlogged=false]')
    # Distinctive corner shop fronts and a vertical colour sign.
    if variant%4==0:
        fill(x+2,2,z-2,x+5,4,z-2,'red_terracotta')
        put(x+3,3,z-3,'sea_lantern')
    elif variant%4==1:
        fill(x+2,1,z-2,x+7,1,z-2,'dark_prismarine')
        fill(x+2,2,z-2,x+7,2,z-2,'orange_stained_glass')
    elif variant%4==2:
        fill(x1-5,2,z-2,x1-2,4,z-2,'blue_concrete')
        put(x1-3,3,z-3,'sea_lantern')
    else:
        fill(x+2,1,z-2,x+8,1,z-2,'cut_copper')
        fill(x+2,2,z-2,x+8,2,z-2,'white_stained_glass')

# Four blocks in each direction. The central 4 by 4 road matrix is contiguous.
sites=[]
for row,(z0,z1) in enumerate(((29,78),(98,146),(166,228),(252,308))):
    cols=(2,3) if row<3 else (0,1,2,3)
    for col in cols:
        x0=(55,156,257,357)[col]
        sites.extend(((x0,z0,33,min(38,z1-z0-3),3+(row+col)%2,row*7+col*3),
                      (x0+40,z0,32,min(36,z1-z0-3),2+(row+col+1)%3,row*9+col*5+1)))
for args in sites:residential_shop(*args)

# Lit stations, street drains, crossing stripes and pocket seating on all roads.
for x in (44,144,243,343,443):
    for z in (61,123,205,282):
        if x>200 or z>224:
            fill(x-8,0,z,x-7,0,z+2,'polished_blackstone')
            fill(x+7,0,z+2,x+8,0,z+4,'polished_blackstone')
for x in (244,344,444):
    for z in (53,116,196,276):
        put(x-12,1,z,'stripped_dark_oak_log')
        fill(x-12,2,z,x-12,9,z,'stripped_dark_oak_log')
        fill(x-13,9,z,x-11,9,z,'dark_oak_fence')
        put(x-11,8,z,'sea_lantern')
for x in (239,339,439):
    for z in (71,137,213,298):
        fill(x,1,z,x+2,3,z,'blue_concrete')
        fill(x,2,z,x+2,2,z,'sea_lantern')
        fill(x,1,z,x+2,1,z,'white_concrete')
for z in (20,88,156,240,322):
    for x in (42,142,242,342,442):
        for xx in range(x-6,x+7,3):
            fill(xx,0,z-1,xx+1,0,z+1,'white_concrete')

# Furnish the hero apartment as small, legible homes rather than one long hall.
# Room 203 uses the same practical layout with a warmer palette; 204 remains
# abandoned, but its covered furniture shows it used to be a home.
def complete_unit(f,j):
    base=f*6;z=30+j*7
    special=(f==1 and j==2)
    vacant=(f==1 and j==3)
    fill(60,base+1,z-1,83,base+4,z+4,AIR)
    floor='spruce_planks' if special else ('dark_oak_planks' if (j+f)%3==0 else 'birch_planks')
    fill(60,base,z-1,83,base,z+4,floor)
    fill(60,base,z-1,62,base,z+4,'polished_deepslate') # genkan
    fill(62,base+1,z+3,64,base+1,z+3,'dark_oak_slab[type=bottom,waterlogged=false]')
    put(61,base+1,z+4,'barrel') # shoes and umbrella
    # Kitchen: lower cabinets, overhead storage, sink, hob and refrigerator.
    fill(64,base+1,z-1,70,base+1,z-1,'smooth_quartz')
    fill(64,base+3,z-1,68,base+3,z-1,'dark_oak_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]')
    put(65,base+1,z-1,'water_cauldron[level=3]')
    put(68,base+1,z-1,'smoker[facing=south,lit=false]')
    fill(64,base+1,z+4,65,base+2,z+4,'iron_block')
    fill(67,base+1,z+2,69,base+1,z+2,'spruce_slab[type=bottom,waterlogged=false]')
    put(68,base+1,z+3,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
    # Half-height entry screen creates a turn into the living room.
    fill(70,base+1,z-1,70,base+2,z,'stripped_dark_oak_log')
    fill(70,base+1,z+3,70,base+2,z+4,'stripped_dark_oak_log')
    fill(71,base+1,z+1,74,base+1,z+1,'light_gray_carpet')
    fill(72,base+1,z+3,74,base+1,z+3,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
    fill(72,base+1,z+2,73,base+1,z+2,'spruce_slab[type=bottom,waterlogged=false]')
    fill(75,base+1,z-1,76,base+2,z-1,'black_concrete') # TV
    fill(75,base+1,z,76,base+1,z,'polished_deepslate_slab[type=bottom,waterlogged=false]')
    # Frosted bedroom partition with a two-block opening at z+1, z+2.
    fill(77,base+1,z-1,77,base+3,z,'white_stained_glass')
    fill(77,base+1,z+3,77,base+3,z+4,'white_stained_glass')
    fill(77,base+4,z-1,77,base+4,z+4,'dark_oak_planks')
    put(80,base+1,z+2,'white_bed[facing=south,part=foot,occupied=false]')
    put(80,base+1,z+3,'white_bed[facing=south,part=head,occupied=false]')
    fill(82,base+1,z-1,83,base+2,z-1,'barrel')
    put(81,base+1,z-1,'bookshelf')
    fill(79,base+1,z-1,80,base+1,z-1,'spruce_slab[type=bottom,waterlogged=false]')
    put(81,base+1,z+4,'flower_pot')
    # Ceiling cove and bedside lamp avoid the flat, unlit slab look.
    fill(62,base+4,z-1,75,base+4,z-1,'spruce_trapdoor[facing=north,half=top,open=false,powered=false,waterlogged=false]')
    put(72,base+4,z+2,'ochre_froglight' if special else 'sea_lantern')
    put(82,base+3,z+3,'lantern[hanging=true,waterlogged=false]')
    if special:
        fill(78,base+1,z+4,81,base+1,z+4,'white_carpet')
        fill(60,base+2,z+3,60,base+3,z+4,'orange_stained_glass')
    if vacant:
        # 204 is deliberately cold and neglected, rather than an empty shell.
        fill(71,base+1,z+1,74,base+1,z+3,'gray_carpet')
        fill(72,base+1,z+3,74,base+1,z+3,'white_wool')
        put(72,base+4,z+2,'redstone_lamp[lit=false]')
        fill(82,base+1,z+3,83,base+2,z+3,'barrel')
    if MODDED:
        furniture(73,base+1,z+3,'gray_couch')
        furniture(72,base+1,z+2,'dark_oak_coffee_table')
        furniture(69,base+1,z-1,'dark_oak_kitchen_cabinet')
        furniture(66,base+1,z-1,'dark_oak_kitchen_sink')
        furniture(83,base+1,z+2,'dark_oak_modern_wardrobe')
        furniture(79,base+1,z-1,'dark_oak_modern_desk')

for floor_index in range(4):
    for unit_index in range(6):complete_unit(floor_index,unit_index)

def renovate_society(x,z,w,d,floors):
    """A lobby, two occupied ground shops and two homes on each upper floor."""
    x1,z1=x+w-1,z+d-1
    cx=x+w//2
    for f in range(floors):
        y=f*5
        # Clear old props inside the existing facade and retain outside walls.
        fill(x+1,y+1,z+1,x1-1,y+4,z1-1,AIR)
        fill(x+1,y,z+1,x1-1,y,z1-1,
             'polished_andesite' if f==0 else 'spruce_planks')
        # Central shared corridor, wall trim and two real apartment/shop doors.
        fill(cx-4,y+1,z+2,cx-4,y+4,z1-2,'light_gray_concrete')
        fill(cx+4,y+1,z+2,cx+4,y+4,z1-2,'light_gray_concrete')
        for side,facing in ((cx-4,'east'),(cx+4,'west')):
            fill(side,y+1,z1-5,side,y+2,z1-5,AIR)
            put(side,y+1,z1-5,f'dark_oak_door[facing={facing},half=lower,hinge=left,open=false,powered=false]')
            put(side,y+2,z1-5,f'dark_oak_door[facing={facing},half=upper,hinge=left,open=false,powered=false]')
        fill(cx-3,y,z+2,cx+3,y,z1-2,'polished_diorite' if f==0 else 'spruce_planks')
        for zz in range(z+5,z1-2,6):put(cx+2,y+4,zz,'sea_lantern')
        for zz in range(z+12,z1-3,7):
            fill(cx-3,y+1,zz,cx-3,y+1,zz+2,'dark_oak_planks')
            fill(cx+3,y+1,zz,cx+3,y+1,zz+2,'dark_oak_planks')
        if f==0:
            fill(cx-3,1,z1-4,cx-3,2,z1-2,'barrel') # lobby mailboxes
            fill(cx+3,2,z1-4,cx+3,3,z1-3,'orange_stained_glass') # notice
            fill(cx-2,1,z1-3,cx-1,1,z1-3,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
        # Ground shops have stocked shelves and cafe seats; upper homes have
        # separate sleeping, sitting and cooking zones.
        if f==0:
            for xx in (x+3,x+6,x1-6,x1-3):
                fill(xx,1,z+4,xx,3,z1-8,'barrel')
            fill(x+3,1,z1-5,x+8,1,z1-5,'smooth_quartz')
            put(x+6,2,z1-5,'water_cauldron[level=3]')
            fill(x1-8,1,z1-7,x1-5,1,z1-7,'spruce_slab[type=bottom,waterlogged=false]')
            put(x1-7,1,z1-5,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
            put(x1-5,1,z1-5,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
            fill(x1-8,1,z+3,x1-4,1,z+3,'dark_oak_planks')
            put(x1-6,2,z+3,'smoker[facing=south,lit=false]')
        else:
            for xx,sign in ((x+3,1),(x1-4,-1)):
                left,right=(x+1,cx-5) if sign==1 else (cx+5,x1-1)
                # Frosted bedroom screen and a clear two-block passage.
                fill(left,y+1,z+10,right,y+3,z+10,'stripped_spruce_log')
                fill(xx,y+1,z+10,xx+1,y+2,z+10,AIR)
                put(xx,y+1,z+4,'white_bed[facing=south,part=foot,occupied=false]')
                put(xx,y+1,z+5,'white_bed[facing=south,part=head,occupied=false]')
                fill(xx+sign,y+1,z+3,xx+sign,y+2,z+3,'barrel')
                fill(xx,y+1,z+7,xx+2,y+1,z+8,'white_carpet')
                fill(left,y+2,z+3,left,y+3,z+4,'bookshelf')
                fill(xx,y+1,z1-6,xx+2,y+1,z1-6,'dark_oak_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]')
                fill(xx,y+1,z1-8,xx+1,y+1,z1-8,'spruce_slab[type=bottom,waterlogged=false]')
                fill(left+1,y+1,z1-9,right-1,y+1,z1-9,'light_gray_carpet')
                fill(left,y+1,z+12,left,y+2,z+12,'black_concrete')
                fill(xx,y+1,z1-3,xx+3,y+1,z1-3,'smooth_quartz')
                put(xx+1,y+1,z1-3,'water_cauldron[level=3]')
                put(xx+2,y+4,z1-9,'ochre_froglight')
                if MODDED:
                    furniture(xx+2,y+1,z1-6,'dark_oak_modern_chair')
                    furniture(xx+2,y+1,z1-3,'dark_oak_kitchen_cabinet')
                    furniture(xx+3,y+1,z+3,'dark_oak_modern_wardrobe')
    # Cut floor openings after furnishing every level so later floor fills do
    # not seal the stairs. Landings connect to the shared centre corridor.
    for f in range(floors-1):
        y=f*5
        fill(cx-1,y+5,z+3,cx+1,y+5,z+8,AIR)
        for step in range(1,6):
            fill(cx-1,y+step,z+3+step,cx+1,y+step,z+3+step,'polished_andesite')
        fill(cx-2,y+1,z+3,cx-2,y+4,z+8,'iron_bars')
    # Restore the accessible double entrance and add a real lobby canopy.
    fill(cx,1,z1,cx+1,2,z1,AIR)
    for xx,hinge in ((cx,'left'),(cx+1,'right')):
        put(xx,1,z1,f'spruce_door[facing=south,half=lower,hinge={hinge},open=false,powered=false]')
        put(xx,2,z1,f'spruce_door[facing=south,half=upper,hinge={hinge},open=false,powered=false]')
    fill(cx-3,4,z1+1,cx+4,4,z1+2,'dark_oak_planks')
    put(cx,3,z1+1,'sea_lantern')
    fill(cx-2,0,z1+1,cx+3,0,z1+4,'polished_andesite')
    # Break the old continuous side-glass strip into occupied bays.
    for f in range(floors):
        y=f*5
        for zz in range(z+3,z1-3,6):
            fill(x,y+1,zz,x,y+4,zz,'stripped_dark_oak_log')
            fill(x1,y+1,zz,x1,y+4,zz,'stripped_dark_oak_log')
            fill(x-1,y+4,zz,x-1,y+4,zz+4,'dark_oak_slab[type=bottom,waterlogged=false]')
            fill(x1+1,y+4,zz,x1+1,y+4,zz+4,'dark_oak_slab[type=bottom,waterlogged=false]')
            if zz%2==0:put(x-1,y+3,zz+2,'lantern[hanging=true,waterlogged=false]')
        fill(x-1,y+1,z+2,x-1,y+1,z1-2,'polished_deepslate_slab[type=bottom,waterlogged=false]')
        fill(x1+1,y+1,z+2,x1+1,y+1,z1-2,'polished_deepslate_slab[type=bottom,waterlogged=false]')

renovate_society(104,29,27,24,3)
renovate_society(5,28,29,19,3)
renovate_society(8,54,25,17,2)

# The filmed elevation has individual entrance bays rather than a repeated
# unbroken wall. The covers stay above head height and the gallery stays open.
for f in range(4):
    y=f*6
    for j in range(6):
        z=30+j*7
        fill(56,y+4,z-1,61,y+4,z+2,'dark_oak_slab[type=bottom,waterlogged=false]')
        put(57,y+3,z+1,'lantern[hanging=true,waterlogged=false]')
        fill(59,y+3,z,59,y+3,z+1,'orange_stained_glass')
        fill(59,y+1,z+3,59,y+3,z+3,'stripped_dark_oak_log')
        fill(59,y+1,z+4,59,y+3,z+5,'gray_terracotta' if j%2 else 'light_gray_terracotta')
        put(57,y+1,z+4,'flower_pot')
        fill(55,y+1,z+1,55,y+1,z+6,'dark_oak_fence')
        for zz in (z+1,z+6):
            fill(55,y+1,zz,55,y+2,zz,'stripped_dark_oak_log')
        fill(55,y+3,z,59,y+3,z,'dark_oak_planks')
    fill(59,y+1,27,59,y+4,27,'stripped_dark_oak_log')
    fill(59,y+1,77,59,y+4,77,'stripped_dark_oak_log')
    for xx in (65,75):
        fill(xx,y+2,27,xx+3,y+3,27,'gray_stained_glass')
        fill(xx,y+2,77,xx+3,y+3,77,'gray_stained_glass')
        fill(xx,y+4,26,xx+3,y+4,26,'dark_oak_slab[type=bottom,waterlogged=false]')
        fill(xx,y+4,78,xx+3,y+4,78,'dark_oak_slab[type=bottom,waterlogged=false]')
for xx in (59,84):
    for zz in (27,77):
        fill(xx,1,zz,xx,23,zz,'stripped_dark_oak_log')

# Mailboxes and street machines read as props rather than coloured cubes.
fill(51,1,46,51,3,49,AIR)
fill(51,1,46,51,2,49,'polished_deepslate')
for zz in (46,47,48,49):put(51,2,zz,'iron_trapdoor[facing=west,half=bottom,open=false,powered=false,waterlogged=false]')
for xx,zz in ((33,45),(34,136),(101,107),(132,175),(155,30),(197,110)):
    fill(xx,1,zz,xx+2,3,zz,'red_concrete')
    fill(xx,2,zz-1,xx+2,2,zz-1,'sea_lantern')
    fill(xx,1,zz-1,xx+2,1,zz-1,'white_concrete')

# The balcony rail must have real openings at every stair departure and
# arrival. These cuts are applied last so facade detailing cannot seal them.
for f in range(3):
    y=f*6;zbase=29+f*13
    fill(55,y+1,zbase,55,y+2,zbase+3,AIR)
    fill(55,y+7,zbase+9,55,y+8,zbase+11,AIR)

S.export()
print('Player arrival: 44,1,92; Room 203 second floor: 65,7,45')
print(f'Town footprint: {S.w} x {S.d}; physical build: {S.h} blocks high')
