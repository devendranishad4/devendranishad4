from pathlib import Path
from PIL import Image, ImageDraw
import random

root = Path('src/main/resources/assets/train31/textures')
(root/'entity').mkdir(parents=True, exist_ok=True)
(root/'gui').mkdir(parents=True, exist_ok=True)
(root/'item').mkdir(parents=True, exist_ok=True)

# Shadow Girl humanoid texture: intentionally original, not based on any copyrighted character.
img = Image.new('RGBA',(64,64),(8,8,10,255)); d=ImageDraw.Draw(img)
# face
for y in range(8,16):
    for x in range(8,16): img.putpixel((x,y),(208,205,201,255))
# hair framing face
for x in range(8,16): img.putpixel((x,8),(8,7,10,255))
for y in range(9,16):
    img.putpixel((8,y),(8,7,10,255)); img.putpixel((15,y),(8,7,10,255))
# eyes
img.putpixel((10,12),(25,25,28,255)); img.putpixel((13,12),(25,25,28,255))
# torso / dress panels
for y in range(20,32):
    for x in range(16,40): img.putpixel((x,y),(9,9,12,255))
# subtle fabric vertical bands
for x in range(17,40,4):
    for y in range(20,32): img.putpixel((x,y),(18,18,22,255))
img.save(root/'entity'/'shadow_girl.png')

# Train texture atlas
img = Image.new('RGBA',(256,256),(62,65,69,255)); d=ImageDraw.Draw(img)
d.rectangle((0,0,255,70), fill=(211,214,215,255))
d.rectangle((0,72,255,104), fill=(23,27,31,255))
d.rectangle((0,106,255,122), fill=(100,8,12,255))
d.rectangle((0,124,255,255), fill=(157,160,163,255))
# grime and panel seams
for x in range(0,256,32): d.line((x,0,x,255), fill=(70,72,74,110), width=1)
random.seed(31)
for _ in range(350):
    x=random.randrange(256); y=random.randrange(256); a=random.randrange(8,32)
    d.point((x,y), fill=(35,35,37,a))
img.save(root/'entity'/'train31.png')

# CCTV scanline overlay
img=Image.new('RGBA',(512,512),(0,0,0,0)); d=ImageDraw.Draw(img)
for y in range(0,512,4): d.rectangle((0,y,511,y+1), fill=(0,0,0,58))
d.rectangle((0,0,511,511), outline=(120,160,145,45), width=8)
img.save(root/'gui'/'cctv_overlay.png')

# Director item: black production controller with red REC lamp and green START key.
img=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(img)
d.rounded_rectangle((2,2,13,13), radius=2, fill=(24,27,31,255), outline=(104,111,118,255))
d.rectangle((4,4,11,6), fill=(9,12,15,255))
d.rectangle((4,8,7,11), fill=(35,165,82,255))
d.rectangle((9,8,11,10), fill=(205,52,52,255))
d.point((10,11), fill=(238,83,83,255))
img.save(root/'item'/'train31_director.png')

# CCTV remote: compact dark remote with four camera keys and a red exit key.
img=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(img)
d.rounded_rectangle((4,1,11,14), radius=2, fill=(18,22,27,255), outline=(95,104,112,255))
d.rectangle((6,3,9,5), fill=(43,155,176,255))
for x,y in [(6,7),(9,7),(6,10),(9,10)]:
    d.rectangle((x,y,x+1,y+1), fill=(203,211,218,255))
d.rectangle((7,12,8,13), fill=(208,57,57,255))
img.save(root/'item'/'cctv_remote.png')
