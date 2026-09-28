"""Render a block-faithful flat-color perspective preview of a generated schematic.

This is an inspection preview, not a Minecraft screenshot or a shader render.
Run with PYTHONPATH=pydeps python3 LastElevatorBuild/render_set_preview.py.
"""
from pathlib import Path
import math
import numpy as np
import nbtlib
from PIL import Image, ImageDraw, ImageFont

BASE=Path('LastElevatorBuild/sets')

def schematic(name):
    root=nbtlib.load(BASE/(name+'.schem'))
    w,h,d=map(int,(root['Width'],root['Height'],root['Length']))
    palette={int(v):str(k) for k,v in root['Palette'].items()}
    raw=bytes((int(i)&255 for i in root['BlockData']))
    ids=[];pos=0
    while pos<len(raw):
        number=0;shift=0
        while True:
            b=raw[pos];pos+=1;number|=(b&127)<<shift;shift+=7
            if not b&128:break
        ids.append(number)
    assert len(ids)==w*h*d
    return np.asarray(ids,dtype=np.uint16).reshape(h,d,w),palette

def color(material):
    p=material.lower()
    if 'air' in p:return (0,0,0)
    for k,v in [('froglight',(247,181,91)),('sea_lantern',(207,237,218)),('shroomlight',(245,153,82)),
                ('redstone_lamp',(175,70,51)),('lantern',(244,173,73)),('tinted_glass',(41,63,82)),
                ('glass',(102,151,167)),('red_carpet',(112,30,34)),('carpet',(95,56,43)),
                ('cut_copper',(147,97,59)),('oxidized_copper',(69,109,99)),('copper',(153,93,59)),
                ('oak_leaves',(48,89,48)),('smooth_quartz',(212,207,190)),('quartz',(218,212,195)),
                ('polished_diorite',(174,171,164)),('white_concrete',(209,209,198)),
                ('diorite',(181,177,168)),('polished_blackstone',(42,42,45)),('blackstone',(38,38,43)),
                ('polished_deepslate',(53,56,61)),('deepslate',(55,57,63)),('dark_oak',(81,52,37)),
                ('spruce',(110,77,49)),('stone_bricks',(105,102,98)),('stone_button',(128,125,120)),
                ('iron_bars',(111,118,121)),('iron_block',(137,144,146)),('red_concrete',(133,36,38)),
                ('gray_concrete',(91,92,92)),('andesite',(112,112,110)),('wool',(207,199,183)),
                ('barrel',(109,74,44)),('sea_lantern',(209,224,206))]:
        if k in p:return v
    return (113,107,100)

def render(name,camera,target,out,title):
    blocks,palette=schematic(name)
    h,d,w=blocks.shape
    colors=np.zeros((max(palette)+1,3),dtype=np.uint8)
    for index,material in palette.items():colors[index]=color(material)
    solid=colors.sum(axis=1)>0
    width,height=720,405
    yy,xx=np.mgrid[0:height,0:width]
    forward=np.asarray(target,dtype=np.float32)-np.asarray(camera,dtype=np.float32)
    forward/=np.linalg.norm(forward)
    right=np.cross(forward,np.array([0,1,0],dtype=np.float32));right/=np.linalg.norm(right)
    up=np.cross(right,forward)
    aspect=width/height;factor=math.tan(math.radians(73)/2)
    directions=(forward[None,None,:]+((xx+.5)/width-.5)[...,None]*2*factor*aspect*right
        +(.5-(yy+.5)/height)[...,None]*2*factor*up).astype(np.float32)
    directions/=np.linalg.norm(directions,axis=2,keepdims=True)
    dx,dy,dz=[directions[:,:,a] for a in range(3)]
    cam=np.asarray(camera,dtype=np.float32)
    xi=np.full((height,width),int(cam[0]),dtype=np.int32)
    yi=np.full((height,width),int(cam[1]),dtype=np.int32)
    zi=np.full((height,width),int(cam[2]),dtype=np.int32)
    sx=np.where(dx>=0,1,-1);sy=np.where(dy>=0,1,-1);sz=np.where(dz>=0,1,-1)
    with np.errstate(divide='ignore',invalid='ignore'):
        tx=np.where(dx>0,(xi+1-cam[0])/dx,(xi-cam[0])/dx)
        ty=np.where(dy>0,(yi+1-cam[1])/dy,(yi-cam[1])/dy)
        tz=np.where(dz>0,(zi+1-cam[2])/dz,(zi-cam[2])/dz)
        stepx=np.abs(1/dx);stepy=np.abs(1/dy);stepz=np.abs(1/dz)
    hit=np.zeros((height,width),dtype=bool)
    rgb=np.zeros((height,width,3),dtype=np.float32)
    rgb[:]=[12,17,23]
    for _ in range(155):
        active=~hit
        if not active.any():break
        ax=active&(tx<=ty)&(tx<=tz)
        ay=active&~ax&(ty<=tz)
        az=active&~ax&~ay
        xi+=sx*ax;yi+=sy*ay;zi+=sz*az
        distance=np.where(ax,tx,np.where(ay,ty,tz))
        tx=np.where(ax,tx+stepx,tx)
        ty=np.where(ay,ty+stepy,ty)
        tz=np.where(az,tz+stepz,tz)
        valid=active&(xi>=0)&(xi<w)&(yi>=0)&(yi<h)&(zi>=0)&(zi<d)
        ids=blocks[np.clip(yi,0,h-1),np.clip(zi,0,d-1),np.clip(xi,0,w-1)]
        found=valid&solid[ids]
        if found.any():
            light=np.where(ay,.95,np.where(ax,.68,.79))
            ambient=np.maximum(.4,1-np.minimum(distance,55)/85)
            shade=light*ambient
            base=colors[ids].astype(np.float32)
            # Slight voxel variation gives depth without inventing texture.
            variation=(((xi*71+yi*41+zi*97)%13)-6)*.006
            shaded=base*(shade+variation)[...,None]
            emissive=(base[:,:,0]>200)&(base[:,:,1]>135)&(base[:,:,2]<140)
            shaded[emissive]=base[emissive]*1.08
            rgb[found]=shaded[found]
            hit|=found
        # Rays leaving the set see a neutral void, never invented city scenery.
        escaped=active&~valid&((xi<0)|(xi>=w)|(yi<0)|(yi>=h)|(zi<0)|(zi>=d))
        hit|=escaped
    image=Image.fromarray(np.uint8(np.clip(rgb,0,255))).resize((1440,810),Image.Resampling.LANCZOS)
    draw=ImageDraw.Draw(image)
    draw.rectangle((0,0,1440,54),fill=(10,13,18))
    draw.text((22,16),title+'  |  BLOCK-FAITHFUL PREVIEW, NOT IN-GAME SCREENSHOT',fill=(237,226,205))
    image.save(out)
    print(out)

if __name__=='__main__':
    render('01_lobby_and_lift',(24.5,2.7,33.5),(23,3.2,13.0),BASE/'01_lobby_actual_blocks_preview.png','HOTEL LOBBY')
    render('01_lobby_and_lift',(23.5,2.7,24.5),(35,2.7,29.0),BASE/'01_lift_actual_blocks_preview.png','LOBBY LIFT')
    render('03_impossible_hotel_13',(10.5,2.3,9.5),(47,2.3,9.5),BASE/'03_hotel_actual_blocks_preview.png','HOTEL FLOOR 13')
