# Singularity

A standalone engine and Android app for real-scale idle space games: one fixed skeleton, scenarios as data, real objects at real distances, 1:1 time.

- Spec: [docs/engine-spec.md](docs/engine-spec.md) · current picks: [docs/decisions.md](docs/decisions.md) · server plan: [docs/server-api.md](docs/server-api.md)
- `engine/` — pure Kotlin, no dependencies: skeleton, rules, pacing solver, scenario loader and validator.
- `scenarios/` — one JSON file per world (72 steps each). The app ships them as assets; CI validates them.
- `app/` — Android app (Compose).

## Adding a scenario
Copy a file in `scenarios/`, keep 6 layers × 12 steps, 6 research names per layer and 3 crew names. Every step needs a real object, a distance (`[value, "km" | "AU" | "ly" | "Mly" | "Gly"]`), a rung (1 known physics, 2 leading theory, 3 hypothesis, 4 ancient) and a beat. CI rejects a step whose top speed breaks its rung.

## Changing the skeleton
Edit `Skeleton.kt`, then `gradle :engine:solveTable` to regenerate `LaunchTable.kt`, then `gradle :engine:test`.

## Checks without Gradle
The engine compiles with plain `kotlinc` (for example the `kotlin-compiler` npm package): compile `engine/src/main/kotlin` with `engine/src/test/kotlin/.../EngineChecks.kt` and call `EngineChecks.all()`.
