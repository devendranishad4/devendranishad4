# The Last Elevator — 0.4.0 preview rebuild

Minecraft Java 1.20.1, Forge 47.4.20, Java 17. Use a **copy** of the supplied Tokyo Inspired City 1.0.10 world. The original 0.3.1 prototype was rejected in a real player test: its lift was static, stages visibly teleported the player, fuses were handed out by a timer, and the scares could occur off camera. Do not film the 0.3.1 build.

This source rebuild puts the lobby, floor 6, hotel floor 13, maintenance, stairs, floor 0 and exit in one existing Tokyo tower. It cuts a vertical shaft through nine original floor levels; a decorated cabin travels one block at a time between landings with shutters closing and opening. The player must press the cabin button. The three fuses are physical pickups. The electrical-room key, fuse panel, service stairs, emergency seal and fire stair are interacted with in sequence. The Passenger appears in the car, chases at maintenance, and reveals itself on floor 0. A separate cold open is recorded after the ending.

## Status

The prior preview source passed a Forge compile. The latest lobby, electrical room, and control corrections require a fresh compile. **No full Minecraft client test or complete recording has passed.** The block preview images are geometry renders using colors from the original map; they are not in-game screenshots. This is still a preview, not a finished video mod.

Some planned script details need client validation or additional work: a live echo of the creator's own recorded voice, the player-head skin on offline accounts, exterior lobby approach and interior visual polish, Passenger line of sight and pathfinding, and a full timed camera-safe playthrough. The cold open is a separate shot to cut to the beginning. The player's spoken lines are performed during recording.

## Intended play route

1. In a **fresh copy** of Tokyo Inspired City 1.0.10, run `/difficulty normal`, `/le tokyo`, `/le check`, `/le setup`.
2. For the cold open shot, run `/le coldopen`, record the corridor sprint, then `/le reset` and `/le setup`.
3. Start the main take with `/le auto 20`. After the 20-second delay, follow the on-screen objective. AUTO stages sounds, clues, scares, and world events; it **never carries you to another floor**. Enter the real cabin and press its floor button to ride. Interact with the breaker, barred door, fuse panel and emergency seal when the objective asks.
4. `/le pause`, `/le resume`, `/le stop`, `/le reset` are recording controls. `/le scene 0..8` is a debug retake command and may start a stage without positioning you; it is not part of the normal take.

The cold open and ending need editing around the main gameplay. Retakes should start with `/le reset`, `/le setup`, then `/le auto 20`.

## Build

GitHub Actions builds `mod/` using the pinned Forge dependency and uploads the JAR. A successful compile checks Java and assets, but only a Minecraft client run can validate real moving-block motion, collision, renderer, lighting, mob navigation, and the full 13–15 minute recording experience.
