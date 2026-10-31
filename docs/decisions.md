# Decisions (night of 2026-10-31)

Picks made while the owner slept, to check in the morning. Each is current state; change any by saying so.

## Skeleton
1. **6 layers × 12 steps = 72 steps**, about 2 years (layer ends on days 60, 180, 300, 420, 600, 730). Layer 1 opens with six steps in the first hour (3, 5, 8, 12, 18, 25 min), then stretches to days and weeks.
2. **Every scenario uses the same economy numbers.** A scenario only changes names, objects, distances and story. So pacing is identical by construction and all scenarios share leaderboards. "Only formulas change" is met by the display formulas: speed = real leg length ÷ flight time.

## Five systems (vertical progression)
1. **Drive:** 4 refits per layer (cap 4, 8 … 24), ×1.35 speed each, paid in the slow currency so it never competes with saving fuel.
2. **Industry:** one endless track per layer (6 tiers), cost ×1.15 per level, output ×2 every 25 levels. Makes the main currency.
3. **Research:** 6 nodes per layer (36), paid in the slow currency; industry ×2/×3 and slow currency ×2. Priced so each layer's nodes are bought across that layer.
4. **Crew:** three AI units hired by the first-hour steps (step 2 launches and drive, step 3 industry, step 5 research). Automation is on once hired; no switches.
5. **Records:** one per step reached, +25% industry each. Permanent.

## Loop
1. Each step: save the launch fuel (main currency), launch, fly. While docked the crew saves fuel first and only buys industry with the surplus; while flying it invests.
2. Launch costs are **solved by the pacing sim** (`Pacing.solve`) so the reference player (full crew from the start) arrives on the reference curve. CI fails if `LaunchTable.kt` is stale or the replay misses the curve by 2 % or more.
3. Offline: 24 h cap, one-minute steps, crew acts as usual. Drift from live play is under 1 % after the first hour.

## Fiction ladder speed limits (validator)
1. Known physics: 200 km/s (Parker Solar Probe reached ~190 km/s).
2. Leading theory: 0.9c.
3. Hypothesis and ancient: no limit.
4. Distances are straight-line from the origin; a step may give its own leg length for moves on a surface or around a planet.

## Scenarios shipped
1. **Earth → MoM-z14** ("The Farthest Light"): the six-act story; rungs 2 → 3 → 4 (event horizon onward). Far distances are comoving distances (Planck-like cosmology, H0 67.7).
2. **Earth → Mars** ("Red Harbor"): no warp, no aliens; rung 1 except the cruise (rung 2, VASIMR-class drive); ends at a lava-tube settlement.
3. A third scenario was left for later; the format and validator are ready for it.

## App
1. New Android app `dev.fanukas.singularity` (Kotlin, Compose), same toolchain as Space Exploration Idle.
2. Flow: start screen → pick a world → play. **Google sign-in is shown but not wired**: it needs a Firebase project and the server (owner setup). "Play on this phone" is the working path.
3. One local save per phone; "Choose another world" in Board deletes it after a confirmation.
4. Tabs: Journey, Industry, Research, Crew, Log (the collection side story), Board (local score until the server exists).
5. **Art:** four fixed holder shapes (icon 256×256, portrait 768×1024, banner 1200×400, scene 1280×720, videos use the scene shape). A drawable with the slot's name fills it; otherwise a labelled placeholder. Slot names: `scn_<id>_banner`, `obj_<id>_<step>`, `ind_<id>_<tier>`, `res_<id>_<node>`, `crew_<id>_<unit>`.

## CI
1. The repo is public, so GitHub Actions minutes are free; still one workflow run per push to main, branches don't build, newer pushes cancel older runs, doc-only pushes skip.
2. Each run: engine tests and pacing report, then the debug APK published as release `build-N`.
