# Claude session rules

## Bugs live in GitHub Issues
Bugs are GitHub Issues with the `bug` label on this repo (fanukas/singularity). Never keep a bug list in a doc.
- Title: `[found N] short description`, N = the APK build it was found in. Open = present in every build from N on.
- Fixing: the commit or PR contains `Fixes #12`. Don't close a bug by hand unless the owner says it's fixed.

## Before pushing
- Run the engine checks in the sandbox first (README, "Checks without Gradle"). Push only when they pass.
- One push to main per change set; CI builds on main only and publishes the APK as release `build-N`.
- Doc-only commits skip CI by path filter; add `[skip ci]` anyway.

## Design
- `docs/engine-spec.md` holds the rules; `docs/decisions.md` the current picks. Keep both current, not historical.
- Never add a scenario-specific number to the engine. Scenarios are data in `scenarios/`.
- A skeleton change means regenerating `LaunchTable.kt` (`gradle :engine:solveTable`).
