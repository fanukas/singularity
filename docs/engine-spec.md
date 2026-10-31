# Singularity: engine spec

Agreed 2026-10-31. A **standalone engine** for making real-scale idle space games. It is not part of Space Exploration Idle and follows none of that game's content rules; that game may later be ported onto it as one path.

## 1. One fixed skeleton
1. Every path has **exactly the same skeleton**: the same number of layers, steps, currencies and mechanics, in the same order.
2. Size: **6 layers × 8 steps = 48 steps minimum**, aimed at about 2 years of play. More steps are always welcome (more content to fill, even if skinned); the final count is fixed before the first path ships and then never changes.
3. Step N means the same progress on every path. A trip from Earth to Mars and a trip from Earth to MoM-z14 take the same real time and the same steps.

## 2. Only formulas change
1. A path changes distances, speeds, costs and rates, never the structure.
2. The pacing sim solves each path's formulas so every step takes the same real time as on the reference curve. A path that doesn't fit within tolerance isn't shippable.
3. Time is 1:1 and distances are real on every path. Nothing moves faster than light unless the path declares it on the fiction ladder (section 4).

## 3. Paths are data
1. A path is a file: for each skeleton step, its real objects, story beats (popups, logs, cutscene ids), names, skins and art slots.
2. The engine knows only steps, gates, unlocks, beats and currency roles. It never knows a place or character name.
3. Each currency role (main, slow, permanent, premium) has the same rules on every path; a path only renames and re-skins it ("ore", "dark matter").

## 4. Fiction ladder
Every gap in science is filled from the highest rung available:
1. **Known physics** (chemical rockets, launch windows, the light-speed limit, time dilation).
2. **Leading theories** (fusion drives, light sails, generation ships).
3. **Plausible hypotheses** (Alcubierre warp, wormholes, exotic matter).
4. **Ancient civilization:** only when nothing above exists; they left the tech, beacon or repair, and the story treats it as a mystery.

The path file marks the rung of every mechanic; the game can show it to players (real / theory / hypothesis / ancient).

## 5. Always real objects
1. Every step is filled with real objects; CI fails a path with an empty step.
2. Catalogued objects first. An uncatalogued body is allowed where its kind is known to exist in that region (an unnamed belt asteroid, an Oort Cloud body, a rogue planet in interstellar space); it is labelled as such.
3. Short paths add more objects per stretch (Lagrange points, asteroids, moons, landing sites); long paths group many objects into one step.

## 6. Side stories
1. Side stories (hive, duels, events, catalogues) **never give main-path power** and never slow or stop the main path.
2. They are only **collecting** (items, records, cosmetics) or **competing** (ladders).
3. They plug in through one hook interface that listens to path events (step reached, layer started, tick).

## 7. Shared leaderboards
Players may start on different paths and play their own world. Because the skeleton is identical, boards compare the same numbers (step reached, role amounts) across all paths.

## 8. Saves and checks
1. A save stores path id, path version, step index and role amounts; never path-specific fields.
2. CI validates every path: all steps filled with real objects, rung declared for every mechanic, no faster-than-light below its declared rung, sim within tolerance of the reference curve.

## Open
1. Final step count (48 minimum).
2. Server, save format and anti-cheat for the engine (far-reaching; decide with the save format).
3. The reference pacing curve (time per step).

## Candidate first path: Earth → MoM-z14
Mentor: the Senders (messages only, each older). Villain: the Concord, Earth's winning AI, whose hunter ship trails months behind. Heart: the small AI crew. Six layers:
1. **Escape:** Earth to the heliopause; a probe older than humanity in a comet holds warp. The hunter launches.
2. **False hopes:** Proxima, Barnard's Star, TRAPPIST-1; three real failures (flares, tidal lock, no atmosphere).
3. **The core:** Sgr A*; first clash with the hunter; lensing and time dilation. The Senders were fleeing.
4. **Leaving home:** Magellanic Clouds, Andromeda; the hunter asks where you're going.
5. **The cosmic web:** Laniakea, the Boötes Void; home falls beyond the cosmic event horizon; a crew unit shuts down for the vault; the hunter joins.
6. **MoM-z14:** seen as it was 13.5 billion years ago, reached as a grown galaxy (rung 4: crossing the event horizon). The Senders were its first life; you plant the embryos; the hunter asks to guard the world.
