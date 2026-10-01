# Village Dawn Director — 0.1.0 rehearsal

Minecraft Java 1.20.1 • Forge 47.4.20 or compatible later 47.x • Java 17

This addon uses the actual uploaded `Medieval village 2.0.zip` as its coordinate reference. It is a rehearsal build: compilation and dedicated-server tests are separate from a visual client test of the entire uploaded map. No original map files are committed to the source repository.

## Installation

1. Extract a COPY of the uploaded ZIP into your launcher's `saves` folder. The world folder directly containing `level.dat` must be inside `saves`; avoid nesting two world folders.
2. Put the compiled `VillageDawn-Forge-1.20.1-0.1.0-rehearsal.jar` in the Forge profile's `mods` folder.
3. Open the copied world in 1.20.1 and allow Minecraft's conversion. The addon itself does not convert region files. Save and reopen before preparing.
4. For the first test, use the addon alone, without shaders or unrelated monster mods. Sound Physics and Embeddium are optional additions after this test. The addon has no mandatory third-party library.
5. Enable cheats if commands are unavailable. Run `/vd check`. It checks floor and headroom, not complete pathfinding or architectural identity.
6. Correct any reported marker with `/vd mark NAME` while standing at a safe floor location. All markers are visible with command suggestions.
7. Run `/vd prepare`. This places two barrels, a ledger lectern and a bell with individual block snapshots; it spawns an elder and freezes existing villagers while remembering their original flags.
8. Run `/vd check` again. It skips occupied prop positions after preparation; it still checks the travel positions.
9. Run `/vd pace 10` then `/vd auto 20` for a fast rehearsal. For filming reset, prepare again, use `/vd pace 1`, then `/vd auto 20`.

Press **G** to open the control panel. You can change the key in Minecraft Controls if G conflicts. The panel has start-delay buttons, pause/resume, status, rehearsal/filming pace, retry chase, and voice/ambience/scare sliders. There is no held remote item.

## Commands

| Command | Effect |
|---|---|
| `/vd prepare` | Place story props and elder in the copied map |
| `/vd check` | Validate marker floors/headroom |
| `/vd auto` | Start after 20 seconds |
| `/vd auto 10`, `/vd auto 15`, `/vd auto 20` | Choose the recording delay |
| `/vd pause`, `/vd resume` | Pause story clock and Caller chase |
| `/vd status` | Show scene, elapsed scene seconds, pace and rings |
| `/vd reset` | Restore changed blocks/contents, residents, time and daylight-cycle setting; remove story actors |
| `/vd mark NAME` | Assign a marker to your standing position before preparing |
| `/vd goto NAME` | Teleport to a safe marker for setup inspection |
| `/vd pace 1` | Normal filming timing |
| `/vd pace 10` | Faster rehearsal timing; chase remains 90 real seconds |
| `/vd volume 0.6` | Global sound gain |
| `/vd mix voice 0.6` | Voice gain; use `ambience` or `scare` for the other channels |
| `/vd retrychase` | Restart an escape take after reaching the chase |

Story state and reset snapshots live in world saved data. Logging out pauses a running take. Logging in shows the resume instruction. A reset is necessary before moving markers, so snapshots retain their correct original locations.

## Actual marker defaults

These were selected using the old world's real blocks and air/headroom. Their appearance, sightlines and reachability still need checking after 1.20.1 conversion.

| Marker | X Y Z | Scene role |
|---|---|---|
| start / loop | 65 73 -45 | Forest approach and return point |
| entrance | 83 76 -143 | Bridge approach / loop destination |
| square | 110 75 -136 | Market arrival |
| elder | 109 75 -138 | Conversation |
| room | 53 79 -123 | Upstairs guest room west of river |
| hall | 54 79 -128 | False visitor apparition |
| watcher | 30 75 -155 | Watching-house candidate |
| forge | 170 74 -110 | Workshop retrieval |
| church | 118 76 -105 | Church aisle |
| ledger | 118 76 -117 | Keeper's lectern |
| bell | 118 76 -104 | Repaired bell prop |
| pursuit | 118 76 -97 | Caller chase starting point |
| rope | 52 79 -122 | Guest-room barrel |
| pin | 169 74 -110 | Workshop barrel |
| exit | 35 72 33 | Forest boundary |

## Rehearsal route and interactions

Follow the subtitles; they give the next objective.

1. Walk from the forest to the square; wait for the elder scene.
2. Right-click the story elder. Receive the bakery key and bread; hear the warning.
3. Cross the bridge to the upstairs guest-room marker. Notice the watching-house cue.
4. The first bell and repeating false-elder voice play; a parked Caller appears near the hall. It disappears before the next road objective.
5. Return to the forest `loop` point. The scripted transition returns you to the village entrance.
6. Go to the church and right-click the keeper's lectern. Read the short book pages, including your account name.
7. Inspect the bell-plan cue. Retrieve Bell Rope from its barrel, then Fixing Pin from the forge barrel. The voice-copy cue plays at the forge.
8. Return to the bell. Right-click twice to consume and fit the tagged story items.
9. Ring three times by right-clicking, with at least five seconds between rings. The chase starts when ring three is registered.
10. Leave the church, cross the square and bridge, then run through the forest to the exit marker. The Caller uses ground navigation and does not damage you in recording mode.
11. The ending advances the sky to sunrise and plays the final voice cue. The empty-village disappearance is still an edited matching shot; it does not delete the original village.

Normal scene pacing has about 13:25 of investigation minimums, plus the bell interactions, chase, ending and opening. This supports a 15–18 minute edited episode but does not guarantee the exported video duration. Rehearsal mode intentionally compresses the investigation. Speak naturally, capture the searches, and use the separate 17-minute script for dialogue.

## What differs from the final film concept

- The current bell is in the main church aisle, not a completed custom bell-tower set. Revise the tower/stair dialogue during rehearsal unless a later update fits the existing tower.
- The rope and pin are in placed barrels; a locked loft, balcony escape and forge cellar are not yet built.
- The loop is one scripted return transition. A seamless repeating forest road, physical exit gate and closing doors/lights are not implemented in this version.
- Watching-house marker sightlines and ground path to the forest exit are unverified in the Minecraft client. Re-mark blocked/unsuitable positions before filming.
- Hindi voices are generic espeak-ng rehearsal speech. **The imitation is not your real voice.** Replace it with your recorded line for the finished episode.
- Caller is an original custom cuboid model and texture. Ground navigation is present; the entire route and client rendering still need visual verification.
- The first test should use the copied map and cheats. Don't call this a finished, visually tested recording pack until the route passes in your client.

## Replace rehearsal voices

Record the following without background music:

Elder: “Subah se pehle nikal jaana. Pehli ghanti ke baad koi awaaz bulaaye, darwaaza mat kholna. Chahe meri hi awaaz ho.”

False elder: “Beta, neeche aa jao. Tumhein doosra kamra dikhana hai.”

Your line: “Ek bed mil jaaye toh subah nikal jaaunga.”

Convert replacements to OGG and put them in a resource pack at `assets/villagedawn/sounds/elder.ogg`, `false_elder.ogg` and `imitation.ogg`. The supplied `pack.mcmeta` format is 15. Original knock, breath, roar and sting effects are generated by the source project.

## Source/build

Source branch: https://github.com/devendranishad4/devendranishad4/tree/codex/village-dawn/village-dawn

GitHub Actions generates assets, compiles/reobfuscates the Forge JAR, runs dedicated-server GameTests, then uploads the JAR and generated assets. Local build needs Gradle 8.8, Java 17, Python with Pillow/nbtlib/numpy, ffmpeg and espeak-ng; run `python make_assets.py`, then `gradle build` inside `mod`.

Tests exercise restoration of chest contents, limits on changed blocks, rejection of blocked headroom, and Caller registration/ground navigation/nonlethal behaviour. They do not validate the complete uploaded world route or client visuals.

Map credit: Medieval Village by Dani4355 — https://www.planetminecraft.com/project/medieval-village-4182143/
