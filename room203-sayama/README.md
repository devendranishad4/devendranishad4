# Room 203 — Sayama edition
Forge 1.20.1 / Forge 47.2.0+, Java 17. Install on both client and server if using a server. One recording player at a time.

1. Keep a backup of your downloaded **Sayama-0.3** save. Open that existing map in Forge 1.20.1.
2. Remove older Last Elevator / Room 203 addon JARs. Put only this version in `.minecraft/mods`.
3. Enable cheats (Open to LAN → Allow Cheats if necessary).
4. Run `/room203 auto`. Close chat and begin recording: you have **20 seconds**. A small top-right objective points to each stair landing, door and chest.

AUTO means automatic events while **you walk** and interact. It places you on the starting street once. There are no scripted teleports after arrival. Adventure mode prevents accidental building damage; the director disables new natural mob spawning, sets rainy midnight and supplies original positional sound effects. A custom neighbour appears, vanishes and later follows you down the stairs without damaging you. It works on Peaceful.

## Commands
| Command | Result |
|---|---|
| `/room203 auto` | Start; 20-second recording delay |
| `/room203 auto 10` | Start; 10-second delay (10–60 accepted) |
| `/room203 reset` | Clean up story actor, reset scenes and restart |
| `/room203 status` | Show current chapter in chat, only when requested |
| `/room203 stop` | Stop story; restore previous time, weather, rules and game mode |
| `/room203 restore` | Stop and restore every block changed by the addon |
| `/room203 volume 80` | Set original story audio volume, 0–100 |
| `/room203 hud off` | Hide objectives for a clean shot |
| `/room203 hud on` | Show objectives again |

Set Minecraft Master and Ambient/Environment volume above zero. Headphones help distinguish the door knocks and footsteps above you. The addon includes eight original sound clips; it needs no furniture or sound dependency mod. It retains the original map's furnishings rather than generating another building.

## Recording locations
Coordinates are block coordinates in the original Sayama v0.3 overworld.
- Start: **680 30 -166**; exit: **682 30 -150**.
- Ground staircase: **671 30 -181**. Follow the real staircase to its landing near **667 36 -177**, open its wooden door and turn along the second-floor corridor.
- Room 203: door **671 36 -200**, bedside chest **667 36 -202**.
- Upper stair entrance: **667 36 -174**. The addon opens this iron door. Follow the U-shaped stairs to **667 41 -173**, then the landing door near **667 41 -178**.
- Room 303: chest **667 41 -202**. Use the same stairs to escape. The final target is the open street, not a lift.

The downloaded map is the setting. This JAR does not contain Sayama or load/download a world for you. It checks the original apartment blocks before applying changes. A failed check leaves the map alone.

## Changes and recovery
Only three invisible light blocks, one bedside lantern, the Room 203 door and the upper stair iron door are changed. Original block states and relevant block-entity data are saved in the world's `room203_edits` SavedData. No fill commands or mass building replacement run, and edits use flags that do not spawn dropped items. Use `/room203 restore` before removing the addon. Your backup also protects against unrelated modifications.

## Map credit
Sayama City v0.3 — Team Sayama, https://www.sayama.city/info/distribution . Obtain the map separately from its authors; this repository and release do not redistribute the world. The optional original Sayama resource pack has separate noncommercial terms: use vanilla textures for monetized recording unless you have appropriate permission. This mod's original code, texture and synthesized sounds are MIT licensed.

## Validation
GitHub Actions compiles the Forge release and runs a real Forge GameTest for wrong-world rejection, idempotent installation, block restoration, furniture preservation, absence of dropped editing items, actor registration and non-damaging behavior, and all eight sound registrations. Offline map inspection checks the intended rooms and stairs. This does not constitute an actual Minecraft client playthrough or a guarantee about every third-party mod/shader combination.
