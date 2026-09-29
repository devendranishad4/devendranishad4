# The Last Elevator — current build

## Tokyo city edition (0.3.0)

This revision stages the story **inside the supplied Tokyo Inspired City 1.0.10 world**. The lobby is a 20-block-high existing atrium, the office is in a glass tower, and Floor 13 uses a furnished, multi-floor Tokyo building with a long corridor, doors and rooms. The mod adds an interior reception, lift, trim, lighting, story props and markers while preserving the city facade and roads. Use **a copy** of the exact map. This is a different visual and spatial result from the earlier standalone blockout.

Install the compiled mod JAR in Minecraft Java 1.20.1 with Forge 47.4.20. In a copy of Tokyo Inspired City 1.0.10, enable commands and run `/difficulty normal`, `/le tokyo`, `/le check`, `/le setup`, `/le auto 20`. `/le tokyo` checks known original map blocks before editing; it refuses a different world. The AUTO story still runs about 14 minutes 45 seconds after the 20-second delay. `/le reset` reseals the emergency exit for a retake. Do not run `/le build` in this workflow; that command is the old independent set builder for an empty plot.

The `tokyo_atrium_lobby_real_blocks_preview.png` render is made from the real Tokyo map block data plus the 0.3.0 placements. It is a flat-color inspection render, not a Minecraft screenshot or shader output. A real client launch and full recording remain necessary before claiming exact visual or runtime fidelity.

Minecraft Java **1.20.1**, Forge **47.4.20**, Java **17**. This package contains six generated Sponge v2 WorldEdit schematics, top-down set plans, mod **source code**, and the story script. It does **not** contain a compiled, game-tested JAR. Do not install the source zip as a mod.

## Status

- Story script: written end to end.
- Lobby, office, hotel, maintenance, floor 0 and stair schematics: generated; NBT headers, dimensions, palette indices, lengths and air-space checks validated locally.
- Passenger and director: the 0.1.0 JAR compiled on GitHub. This 0.2.0 rebuild adds a more detailed reception, marble floor pattern, wall and ceiling trim, suite fronts, lit corridors, and a repaired fuse marker. Runtime filming still needs a real client check.
- Original synthesised lift, breathing, ambience and scare audio is included; spoken lines still need recording.
- Tokyo Inspired City world: inspected; it is Minecraft 1.20.1. The build has **not** been pasted into or saved over the original world.
- Current limitation: GitHub compiles the JAR, but a full client recording and shader/map inspection have not been completed. The generated images remain visual targets; the sets are block-built layouts, not pixel-identical replicas.

## Set files

`sets/01_lobby_and_lift.schem`, `02_office_and_fuse_panel.schem`, `03_impossible_hotel_13.schem`, `04_maintenance_chase.schem`, `05_floor_zero_and_exit.schem`, `06_looping_stairwell.schem`.

The source includes an in-mod `/le build` command that reads the schematics and refuses to overwrite existing blocks. Stand on a fully empty **at least 72 × 52 block** plot with 52 blocks of clear height, in a copy of your world. It stacks the sets vertically, places the stairwell beside them, saves all markers and moves you to the lobby. `/le build` still needs an in-game runtime test. Avoid a WorldEdit paste over existing city blocks; air in a normal paste would overwrite the selected cuboid. The `*_plan.png` files are **top-down plans**; `*_actual_blocks_preview.png` files are flat-color perspective renders of the real block schematics, not Minecraft screenshots. Neither is the generated concept art; shaders, scenery beyond the plot, and resource-pack details are absent.

## Mod build when dependencies are available

The GitHub Actions build compiles `mod/` with Java 17 and Forge 47.4.20 and uploads the JAR. A successful compile validates the mod's Java/API usage; a game launch and full scene run are separate checks.

## Director commands planned in source

`/le build` is intended to set all markers automatically. For hand-placed or modified sets, stand at each actual location and use `/le mark <name>`. Required names: `lobby`, `car`, `office`, `hotel`, `maintenance`, `stair`, `zero`, `street`, `fuse1`, `fuse2`, `fuse3`, `passenger_maintenance`, `passenger_zero`. Every arrival marker needs a solid floor and two blocks of clear space. The `car` marker is for the elevator's button proximity check.

Run `/le build` on an empty lot, then `/le check`, `/le setup`, `/le auto 20` for a timed recording pass. The mod schedules nine scenes over approximately 14 minutes 45 seconds after a 20-second countdown and awards the three fuses during that run. The cold open should be recorded as a separate shot and edited to the beginning. Save a copy of the world before using `/le build` and walk through it once before filming.

For manual control, use `/le start 20`, then `/le next` or the marked car's stone button for scenes 1–8. `pause`, `resume`, `stop`, `reset`, `/le scene 0..8` and `/le fuse 1..3` support retakes. `/le scene` bypasses the fuse gate for testing. The Passenger does no damage. Do not use `/le auto` for a final recording before path and timing checks in the selected world.

## Remaining acceptance checks

1. Compile source; fix actual Forge API/compiler errors if any.
2. Select and prepare a tower location in a **copy** of the city world; paste and inspect all six sets in game.
3. Mark each arrival, fuse, and passenger spawn; test safe teleport points and elevator doors.
4. Record every scene including the alternate look-back scare, repeat stair landing, chase path and exit.
5. Add original bell, footstep and radio audio assets; verify levels and no repeated audio.
6. Make a successful full 13–15 minute recording before calling this a final mod.

The current source intentionally leaves the actual elevator door animation, the stair-loop teleport, rule-specific look-back scare, world-block restoration and visual employee-photo replacement for the integrated map pass. These are substantial, not cosmetic, remaining tasks.
