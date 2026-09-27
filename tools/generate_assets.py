from pathlib import Path
from PIL import Image, ImageDraw

root = Path('src/main/resources/assets/train31/textures')
(root/'entity').mkdir(parents=True, exist_ok=True)
(root/'gui').mkdir(parents=True, exist_ok=True)
(root/'item').mkdir(parents=True, exist_ok=True)

# Horror girl inspired by the supplied reference: small pale girl, long black hair,
# dark red dress and an unsettling face. Original Minecraft skin texture; no copied artwork.
img = Image.new('RGBA',(64,64),(8,7,9,255))
d = ImageDraw.Draw(img)
PALE=(202,198,193,255)
PALE_SHADOW=(168,164,161,255)
HAIR=(7,6,9,255)
HAIR_HI=(18,16,20,255)
DRESS=(105,16,25,255)
DRESS_DARK=(62,9,15,255)
EYE=(18,17,20,255)

# Head base regions.
d.rectangle((0,0,31,15), fill=PALE)
# Front face (standard skin front: 8..15,8..15).
d.rectangle((8,8,15,15), fill=PALE)
d.rectangle((8,8,15,9), fill=HAIR)
d.rectangle((8,9,9,15), fill=HAIR)
d.rectangle((14,9,15,15), fill=HAIR)
d.rectangle((10,10,13,10), fill=PALE_SHADOW)
d.point((10,12), fill=EYE); d.point((13,12), fill=EYE)
d.point((11,14), fill=(105,74,76,255)); d.point((12,14), fill=(105,74,76,255))
# Hair on other head faces/top.
d.rectangle((0,0,31,7), fill=HAIR)
d.rectangle((0,8,7,15), fill=HAIR)
d.rectangle((16,8,31,15), fill=HAIR)
# Outer head/hat layer: long messy hair framing the face.
d.rectangle((32,0,63,15), fill=(0,0,0,0))
d.rectangle((40,0,47,7), fill=HAIR)
d.rectangle((32,8,39,15), fill=HAIR)
d.rectangle((48,8,55,15), fill=HAIR)
d.rectangle((40,8,41,15), fill=HAIR)
d.rectangle((46,8,47,15), fill=HAIR)
for y in range(10,16):
    if y % 2 == 0:
        d.point((42,y), fill=HAIR_HI)
        d.point((45,y), fill=HAIR_HI)

# Torso = dark red dress.
d.rectangle((16,16,39,31), fill=DRESS)
d.rectangle((20,20,27,31), fill=DRESS)
d.rectangle((20,20,27,22), fill=(132,22,32,255))
d.rectangle((16,28,39,31), fill=DRESS_DARK)
# Right arm skin + red sleeve.
d.rectangle((40,16,55,31), fill=PALE)
d.rectangle((40,20,55,27), fill=DRESS)
d.rectangle((40,28,55,31), fill=PALE_SHADOW)
# Right leg: dress continues downward, dark shoes at bottom.
d.rectangle((0,16,15,31), fill=DRESS_DARK)
d.rectangle((0,28,15,31), fill=(20,18,21,255))
# Left leg + arm second-layer mappings.
d.rectangle((16,48,31,63), fill=DRESS_DARK)
d.rectangle((16,60,31,63), fill=(20,18,21,255))
d.rectangle((32,48,47,63), fill=PALE)
d.rectangle((32,52,47,59), fill=DRESS)
d.rectangle((32,60,47,63), fill=PALE_SHADOW)
# A few hair-like vertical streaks over shoulders/back.
for x in (17,19,36,38):
    d.rectangle((x,20,x,30), fill=HAIR)

img.save(root/'entity'/'shadow_girl.png')

# Final form: same girl, stronger contrast and dark-red staining, still non-graphic.
final = img.copy(); fd=ImageDraw.Draw(final)
for box in [(21,24,23,29),(25,20,27,24),(2,22,5,27),(34,55,37,60),(10,10,10,13),(13,10,13,13)]:
    fd.rectangle(box, fill=(66,5,12,235))
fd.point((10,12), fill=(224,224,219,255)); fd.point((13,12), fill=(224,224,219,255))
fd.rectangle((8,15,15,15), fill=(44,8,12,255))
final.save(root/'entity'/'shadow_girl_final.png')

# Uniform train materials.
def solid(name, rgba):
    Image.new('RGBA',(512,512),rgba).save(root/'entity'/(name+'.png'))
solid('train_body',(220,226,228,255))
solid('train_roof',(115,122,126,255))
solid('train_window',(16,25,30,255))
solid('train_stripe',(40,164,170,255))
solid('train_dark',(32,36,39,255))
solid('train_light',(255,238,190,255))

# CCTV scanline overlay.
img=Image.new('RGBA',(512,512),(0,0,0,0)); d=ImageDraw.Draw(img)
for y in range(0,512,4): d.rectangle((0,y,511,y+1), fill=(0,0,0,58))
d.rectangle((0,0,511,511), outline=(120,160,145,45), width=8)
img.save(root/'gui'/'cctv_overlay.png')

# Director item.
img=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(img)
d.rounded_rectangle((2,2,13,13), radius=2, fill=(24,27,31,255), outline=(104,111,118,255))
d.rectangle((4,4,11,6), fill=(9,12,15,255)); d.rectangle((4,8,7,11), fill=(35,165,82,255)); d.rectangle((9,8,11,10), fill=(205,52,52,255)); d.point((10,11), fill=(238,83,83,255))
img.save(root/'item'/'train31_director.png')

# CCTV remote.
img=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(img)
d.rounded_rectangle((4,1,11,14), radius=2, fill=(18,22,27,255), outline=(95,104,112,255))
d.rectangle((6,3,9,5), fill=(43,155,176,255))
for x,y in [(6,7),(9,7),(6,10),(9,10)]: d.rectangle((x,y,x+1,y+1), fill=(203,211,218,255))
d.rectangle((7,12,8,13), fill=(208,57,57,255))
img.save(root/'item'/'cctv_remote.png')
