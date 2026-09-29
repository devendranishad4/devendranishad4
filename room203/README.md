# Room 203 district for the user's Tokyo map

This source adds an original 480 × 344 block residential filming district to a private copy of a user-supplied `Tokyo Inspired City 1.0.10` world (Java 1.20.1). The Tokyo map is by CoinCoffer, is licensed All Rights Reserved, and is **not** included in this repository. The output's world border is 4,096 × 4,096 blocks. Its original city core is preserved; much of the rest of that area is natural terrain.

Dependencies: Python 3, `nbtlib`, `Pillow`. In this directory run:

```bash
python install_into_tokyo.py "/path/to/Tokyo Inspired City 1.0.10"
ROOM203_MACAW=1 python install_into_tokyo.py "/path/to/Tokyo Inspired City 1.0.10"
```

The optional Macaw edition requires Macaw's Furniture 3.4.1 for Minecraft Forge 1.20.1, installed before opening the world. Builders call `build_map_v3.py`, which writes a Sponge v2 `.schem`. This code has not been checked in a running Minecraft client; it was validated by reading Anvil chunk block states, tile entities, metadata and ZIP integrity.
