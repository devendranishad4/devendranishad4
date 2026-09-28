# The Last Elevator — current build

Minecraft Java **1.20.1**, Forge **47.4.20**, Java **17**. This package contains six generated Sponge v2 WorldEdit schematics, top-down set plans, mod **source code**, and the story script. It does **not** contain a compiled, game-tested JAR. Do not install the source zip as a mod.

## Status

- Story script: written end to end.
- Lobby, office, hotel, maintenance, floor 0 and stair schematics: generated; NBT headers, dimensions, palette indices, lengths and air-space checks validated locally.
- Passenger and director: earlier build compiled; the latest audio revision and actual Forge runtime test remain pending.
- Original synthesised lift, breathing, ambience and scare audio is included; spoken lines still need recording.
- Tokyo Inspired City world: inspected; it is Minecraft 1.20.1. The build has **not** been pasted into or saved over the original world.
- Current limitation: GitHub compiles the JAR, but a full client recording and shader/map inspection have not been completed. The generated images remain visual targets; the sets are block-built layouts, not pixel-identical replicas.

## Set files

`sets/01_lobby_and_lift.schem`, `02_office_and_fuse_panel.schem`, `03_impossible_hotel_13.schem`, `04_maintenance_chase.schem`, `05_floor_zero_and_exit.schem`, `06_looping_stairwell.schem`.

The source now includes an in-mod `/le build` command that reads the schematics and refuses to overwrite existing blocks. Stand on a fully empty **at least 72 × 52 block** plot with 52 blocks of clear height, in a copy of your world. It stacks the sets vertically, places the stairwell beside them, saves all markers and moves you to the lobby. `/le build` must still be compiled and tested in-game. Avoid a WorldEdit paste over existing city blocks; air in a normal paste would overwrite the selected cuboid. The preview PNGs are **top-down plans**, not game screenshots.

## Mod build when dependencies are available

The GitHub Actions build compiles `mod/` with Java 17 and Forge 47.4.20 and uploads the JAR. A successful compile validates the mod's Java/API usage; a game launch and full scene run are separate checks.

## Director commands planned in source

`/le build` is intended to set all markers automatically. For hand-placed or modified sets, stand at each actual location and use `/le mark <name>`. Required names: `lobby`, `car`, `office`, `hotel`, `maintenance`, `stair`, `zero`, `street`, `fuse1`, `fuse2`, `fuse3`, `passenger_maintenance`, `passenger_zero`. Every arrival marker needs a solid floor and two blocks of clear space. The `car` marker is for the elevator's button proximity check.

Run `/le build` on an empty lot, then `/le check`, `/le setup`, `/le auto 20` for a fully timed recording pass. The mod schedules nine scenes over approximately 13 minutes after a 20-second countdown and awards the three fuses during that run. The cold open should be recorded as a separate shot and edited to the beginning. Save a copy of the world before using `/le build` and walk through it once before filming.

For manual control, use `/le start 20`, then `/le next` or the marked car's stone button for scenes 1–8. `pause`, `resume`, `stop`, `reset`, `/le scene 0..8` and `/le fuse 1..3` support retakes. `/le scene` bypasses the fuse gate for testing. The Passenger does no damage. Do not use `/le auto` for a final recording before path and timing checks in the selected world.

## Remaining acceptance checks

1. Compile source; fix actual Forge API/compiler errors if any.
2. Select and prepare a tower location in a **copy** of the city world; paste and inspect all six sets in game.
3. Mark each arrival, fuse, and passenger spawn; test safe teleport points and elevator doors.
4. Record every scene including the alternate look-back scare, repeat stair landing, chase path and exit.
5. Add original bell, footstep and radio audio assets; verify levels and no repeated audio.
6. Make a successful full 13–15 minute recording before calling this a final mod.

The current source intentionally leaves the actual elevator door animation, the stair-loop teleport, rule-specific look-back scare, world-block restoration and visual employee-photo replacement for the integrated map pass. These are substantial, not cosmetic, remaining tasks.
