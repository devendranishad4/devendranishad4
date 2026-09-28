# THE LAST ELEVATOR — recording script and mod contract

Minecraft Java 1.20.1 Forge · solo gameplay · target 13–15 minutes · Hindi/Hinglish voice-over. One detailed city tower, one elevator car, three strange floor sets and a service stairwell. No other actors required.

## Core rule and ending

The elevator has a brass plaque: **“If a passenger enters after the bell, do not look at their face.”** A black-clothed passenger appears behind the player in reflections and through door gaps. The building is trying to make the player take that passenger's place. Escaping requires collecting three maintenance fuses and reaching the service exit before the elevator returns to **floor 0**. Floor 0 is a blank white corridor: it is not the lobby.

The viewer sees the entire escape in this video. Final sting: outside, the tower directory shows the player's name as its new night operator.

## Recording layout

Build/choose a detailed tower with a furnished lobby, an elevator car with doors, a normal office floor, an empty hotel floor, a dark maintenance floor, a white corridor and an exterior street. Each set must be physically separated so a closed elevator door hides teleportation. Place readable signs, working lights and props in the sets. Do not use flat generated rooms. The mod must use markers placed in the actual chosen map, rather than assume fixed world coordinates.

Camera: first-person gameplay for exploration/chases; optional Replay Mod shots only for opening and two reveals. The player records alone. The director starts after a configurable 10/15/20-second delay (default 20 seconds), with a clear countdown that ends before filming begins.

## Complete scene script and cue sheet

Times are editing targets, not a timer that drags the player through scenes. The director waits for explicit /le next or an in-world trigger at each transition. Each trigger is one-shot and resumable.

### 0:00–0:25 — Cold open (record this after the ending)

Player sprints down a dim corridor. Elevator doors open behind him; the floor indicator shows **0**. Three quick bell strikes. Cut before the passenger reaches him.

**Voice:** “Main toh ground floor pe tha… phir lift ne zero floor kahan se dikhaya?”

On-screen title, then hard cut to earlier that night. Mod cues `cold_open`: brief bell x3, indicator 0, silhouette at far end, chase cue. No sudden teleport in the recorded first-person shot.

### 0:25–1:50 — A normal building

Late-night arrival at a believable city tower. Security desk is empty; clock says 11:47 PM. A note says maintenance office is on floor 6. Player enters lift.

**Voice:** “Ek maintenance call ke liye aaya tha. Reception pe koi nahi, par lights sab on thi.”

Mod cues `lobby`: city ambience, lobby lights, call button usable. The doors stay closed until the player presses the button.

### 1:50–3:00 — Floor 6: a small contradiction

Ordinary office floor. Maintenance panel needs three fuses. The first fuse sits in a drawer. Printer produces a slip: **“DON'T RETURN WITH TWO PEOPLE.”** The player hears a bell from the empty elevator.

**Voice:** “Panel mein teen fuse missing the. Par ye paper kisne print kiya?”

Mod cues `office`: lights dip once, printed paper appears, first fuse collectible, elevator bell after pickup. Never spawn the passenger in sight of the camera.

### 3:00–4:25 — The floor that should not exist

Player presses lobby. Doors open to an abandoned hotel corridor marked **13**; the tower directory had no floor 13. The lift closes when the player turns around. A guest room is set for one person but holds two cups. Second fuse in its service closet.

**Voice:** “Is building mein floor thirteen hai hi nahi. Aur mere aane se pehle yahan do cup kisne rakhe?”

Mod cues `hotel`: destination teleport only behind closed elevator doors; distant rolling-cart sound, room key in closet, fuse pickup. Exit opens when the second fuse is taken.

### 4:25–6:05 — The rule

Service note beside the lift: **“AFTER THE BELL, DO NOT LOOK AT THE OTHER PASSENGER.”** Bell rings. Two sets of footsteps play, although the player is alone. Through a narrow door gap, black shoes stand inside the lift. On direct view the car appears empty. The player boards and looks at the floor panel instead of behind him.

**Voice (quiet):** “Maine bell ke baad kisi ko andar aate suna… main peeche nahi mud raha.”

Mod cues `rule`: sound positioned behind player, silhouette only after line-of-sight checks, no forced camera movement. If the player looks back, play an alternate scare and continue the same story; never soft-lock progression.

### 6:05–8:00 — Maintenance floor

Lift opens to a dark floor of cables, pipes and emergency lamps. Radio repeats the player's earlier sentence in his own voice. Third fuse lies in a locked electrical room. Find a key behind a breaker; lights go out after pickup, emergency lamps switch on and the passenger begins pursuing from the far hallway.

**Voice:** “Ye meri awaaz hai. Maine ye baat abhi sirf lift mein boli thi.”

Mod cues `maintenance`: radio recording, key, fuse, blackout limited to the set, glow strips to guide route, chase begins after the player has a clear exit. Chase must be path-tested in the selected map.

### 8:00–9:40 — Return to floor 6

Player reaches office and installs all three fuses. Panel powers up but warns: **“One passenger must remain.”** The lift arrives; its bell rings. A figure stands inside. Player takes the service stair instead. Stairwell doors repeatedly open to the same landing, each time with one fewer working lamp.

**Voice:** “Agar main lift mein gaya toh shayad main hi yahan reh jaunga. Seedhiyan try karta hoon.”

Mod cues `loop`: inspect all three fuse inventory flags; office restore; 2–3 stair loops with changing signs/lights; no unbounded teleport loop.

### 9:40–11:30 — Floor 0

Stairwell finally reaches floor **0**, a silent white corridor. A wall of employee photos includes the player's current skin. The passenger emerges slowly from a door at the far end. The only visible way out is a marked fire exit with a breakable emergency seal.

**Voice:** “Yahan meri photo kaise lagi hai? Main toh aaj pehli baar aaya hoon.”

Mod cues `zero`: safe arrival, photo prop placeholder until the actual skin is known, passenger reveal behind a marker, 8-second pursuit, emergency seal interactable. No permanent full-screen blindness.

### 11:30–13:20 — Escape and payoff

Player breaks seal, races down the final stair and exits to the street as morning light appears. Tower is quiet. He looks back at the building directory: **“NIGHT OPERATOR: [player name].”** One elevator bell rings from a high window. Cut to black.

**Voice:** “Main bahar aa gaya… lekin us building ko mera naam kaise pata hai?”

Mod cues `escape`: seal breaks only after fuse and loop phases, exit stays available, daylight exterior, name rendered from actual player name. Fade is done in editing, not by forcing a black screen in-game.

## Director mod: required behavior

- Forge 1.20.1, Java 17, one player in a dedicated recording world. No extra actors or voice dependencies.
- `/le mark <lobby|car|office|hotel|maintenance|stair|zero|street|passenger_spawn|fire_exit>` saves location/direction in world persistent data. `/le check` prints missing markers and safe-room checks.
- `/le setup` creates only story-owned props inside small, previewable regions; backs up touched blocks and restores them with `/le reset`. It never rewrites the rest of the map.
- `/le start [10|15|20]` countdown, default 20; `/le pause`, `/le resume`, `/le stop`, `/le reset` and `/le scene <name>` support solo retakes. `/le next` is a manual scene advance if an interaction fails.
- Doors must visually close **before** a floor teleport. Teleport positions must be verified for two blocks of air and solid floor; on failure keep the player in the car and report the blocked marker.
- Scene transitions depend on actual item pickup/button interaction or manual advance, never on elapsed time alone. Small ambience events may use ticks and can be skipped on pause. One-shot flags prevent duplicate pickups, audio spam and repeated scares.
- Ghost: one custom passenger model/skin, controlled spawn points and a state machine (hidden → glimpse → pursuit → despawn); avoid constant random spawning. Attack must stop short of killing the player during a take, with an optional retake reset.
- Server-side data persists stage, markers, fuses and setup backup. Stop/despawn cleanup is deterministic. No forced spectator mode, camera lock, fog covering the UI, irreversible world edits or unbounded command loops.
- Audio cues: elevator motor, single bell, footsteps, radio echo, electrical hum, chase sting. Dialogue/voice acting is recorded separately. Resource pack volume needs a user control. All sound assets must be original or licensed.
- `/le test <scene>` validates each individual beat before a full dry run. Log event IDs with timestamps. A full test should replay setup → all fuses → alternate look-back scare → stair loop → final exit → reset, including relog after scene 3.

## Acceptance gate before calling the mod finished

Compile the Forge JAR; launch with the exact Forge build and intended mod pack; inspect every set and path in the user's chosen world; capture a complete solo test recording; confirm no broken trigger, collision, sound spam, premature jump scare or lost exit; then deliver the JAR, source, map coordinates and exact in-game commands. Until those checks pass, a source project is a prototype, not a finished recording mod.
