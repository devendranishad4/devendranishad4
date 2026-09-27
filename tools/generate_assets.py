from pathlib import Path
from PIL import Image, ImageDraw
import random

root = Path('src/main/resources/assets/train31/textures')
(root/'entity').mkdir(parents=True, exist_ok=True)
(root/'gui').mkdir(parents=True, exist_ok=True)
(root/'item').mkdir(parents=True, exist_ok=True)

# Base Shadow Girl: pale face, long dark hair and dark dress. Original texture.
img = Image.new('RGBA',(64,64),(8,8,10,255)); d=ImageDraw.Draw(img)
for y in range(8,16):
    for x in range(8,16): img.putpixel((x,y),(205,202,198,255))
for x in range(8,16): img.putpixel((x,8),(7,6,9,255))
for y in range(9,16):
    img.putpixel((8,y),(7,6,9,255)); img.putpixel((15,y),(7,6,9,255))
img.putpixel((10,12),(20,20,23,255)); img.putpixel((13,12),(20,20,23,255))
for y in range(20,32):
    for x in range(16,40): img.putpixel((x,y),(9,9,12,255))
for x in range(17,40,4):
    for y in range(20,32): img.putpixel((x,y),(18,18,22,255))
img.save(root/'entity'/'shadow_girl.png')

# Final form: same character with non-graphic dark-red staining and stronger contrast.
final = img.copy(); d=ImageDraw.Draw(final)
for box in [(9,10,11,13),(13,14,15,15),(18,22,23,28),(30,20,36,26),(17,29,20,31),(35,27,39,31)]:
    d.rectangle(box, fill=(91,14,20,220))
d.rectangle((9,12,10,12), fill=(225,225,220,255)); d.rectangle((13,12,14,12), fill=(225,225,220,255))
final.save(root/'entity'/'shadow_girl_final.png')

# Uniform train materials. Keeping each texture a solid field avoids stretched block-artifacts
# and lets the custom model read as a proper white/turquoise Tokyo commuter train.
def solid(name, rgba):
    Image.new('RGBA',(512,512),rgba).save(root/'entity'/(name+'.png'))
solid('train_body',(220,226,228,255))
solid('train_roof',(115,122,126,255))
solid('train_window',(16,25,30,255))
solid('train_stripe',(40,164,170,255))
solid('train_dark',(32,36,39,255))
solid('train_light',(255,238,190,255))

# CCTV scanline overlay
img=Image.new('RGBA',(512,512),(0,0,0,0)); d=ImageDraw.Draw(img)
for y in range(0,512,4): d.rectangle((0,y,511,y+1), fill=(0,0,0,58))
d.rectangle((0,0,511,511), outline=(120,160,145,45), width=8)
img.save(root/'gui'/'cctv_overlay.png')

# Director item
img=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(img)
d.rounded_rectangle((2,2,13,13), radius=2, fill=(24,27,31,255), outline=(104,111,118,255))
d.rectangle((4,4,11,6), fill=(9,12,15,255)); d.rectangle((4,8,7,11), fill=(35,165,82,255)); d.rectangle((9,8,11,10), fill=(205,52,52,255)); d.point((10,11), fill=(238,83,83,255))
img.save(root/'item'/'train31_director.png')

# CCTV remote
img=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(img)
d.rounded_rectangle((4,1,11,14), radius=2, fill=(18,22,27,255), outline=(95,104,112,255))
d.rectangle((6,3,9,5), fill=(43,155,176,255))
for x,y in [(6,7),(9,7),(6,10),(9,10)]: d.rectangle((x,y,x+1,y+1), fill=(203,211,218,255))
d.rectangle((7,12,8,13), fill=(208,57,57,255))
img.save(root/'item'/'cctv_remote.png')
