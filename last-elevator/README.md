# The Last Elevator — current build

Minecraft Java **1.20.1**, Forge **47.4.20**, Java **17**. This package contains six generated Sponge v2 WorldEdit schematics, top-down set plans, mod **source code**, and the story script. It does **not** contain a compiled, game-tested JAR. Do not install the source zip as a mod.

## Status

- Story script: written end to end.
- Lobby, office, hotel, maintenance, floor 0 and stair schematics: generated; NBT headers, dimensions, palette indices, lengths and air-space checks validated locally.
- Passenger and director: source implementation; compilation and actual Forge runtime testing remain pending.
- Tokyo Inspired City world: inspected; it is Minecraft 1.20.1. The build has **not** been pasted into or saved over the original world.
- Current limitation: Gradle distribution and Forge dependencies could not be fetched in this environment, so no JAR could be compiled or tested. The scenes are also not yet anchored to a selected tower footprint; generated images remain visual targets.

## Set files

`sets/01_lobby_and_lift.schem`, `02_office_and_fuse_panel.schem`, `03_impossible_hotel_13.schem`, `04_maintenance_chase.schem`, `05_floor_zero_and_exit.schem`, `06_looping_stairwell.schem`.

Do not paste directly over existing city blocks. Paste the sets in empty world space or a copy of the city world; test entry, exits and light levels before assigning markers. Air is included in the schematics, so a normal paste overwrites the selected cuboid. The preview PNGs are **top-down plans**, not game screenshots.

## Mod build when dependencies are available

Open `mod/` on a Windows computer with Java 17 and internet access to Forge's Maven and Gradle hosts, then run `gradlew.bat build`. The expected JAR goes in `mod/build/libs/`. This is a source build attempt until the JAR compiles and completes an in-game test.

## Director commands planned in source

Stand at each actual location and mark it with `/le mark <name>`. Required names: `lobby`, `car`, `office`, `hotel`, `maintenance`, `stair`, `zero`, `street`, `fuse1`, `fuse2`, `fuse3`, `passenger_maintenance`, `passenger_zero`. Every arrival marker needs a solid floor and two blocks of clear space. The `car` marker is for the elevator's button proximity check.

Use `/le check`, `/le setup`, `/le auto 20` for a fully timed recording pass. The mod source schedules nine scenes over approximately 13 minutes after a 20-second countdown and awards the three fuses during that run. The cold open should be recorded as a separate shot and edited to the beginning. **AUTO is source only until a JAR builds and is tested in-game.**

For manual control, use `/le start 20`, then `/le next` or the marked car's stone button for scenes 1–8. `pause`, `resume`, `stop`, `reset`, `/le scene 0..8` and `/le fuse 1..3` support retakes. `/le scene` bypasses the fuse gate for testing. The Passenger does no damage. Do not use `/le auto` for a final recording before path and timing checks in the selected world.

## Remaining acceptance checks

1. Compile source; fix actual Forge API/compiler errors if any.
2. Select and prepare a tower location in a **copy** of the city world; paste and inspect all six sets in game.
3. Mark each arrival, fuse, and passenger spawn; test safe teleport points and elevator doors.
4. Record every scene including the alternate look-back scare, repeat stair landing, chase path and exit.
5. Add original bell, footstep and radio audio assets; verify levels and no repeated audio.
6. Make a successful full 13–15 minute recording before calling this a final mod.

The current source intentionally leaves the actual elevator door animation, the stair-loop teleport, rule-specific look-back scare, world-block restoration and visual employee-photo replacement for the integrated map pass. These are substantial, not cosmetic, remaining tasks.
