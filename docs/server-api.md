# Server API (planned, not built)

One server for every scenario: the calls are the same, only the data differs. Owner setup needed first: a Firebase project for Google sign-in, and a Cloud Run service (the same pattern as Space Exploration Idle).

1. `POST /v1/session` — Firebase ID token in. Returns the account id and the server time.
2. `GET /v1/save` — the account's save (`GameState` JSON, including `scenario` and `scenarioVersion`).
3. `PUT /v1/save` — upload a save. The server replays the gap with the same engine (`Engine.offline`, 24 h cap) from the last checked save and refuses values the rules can't reach. Scenario files are never needed for this: the economy has no scenario-specific numbers.
4. `GET /v1/board?top=100` — one shared board for all scenarios, ranked by `Engine.progressScore` (step plus share of the current leg), then lifetime main currency. Each row shows the scenario id, so players see who flies which world.
5. Side stories (duels, events, collections) get their own routes later and never write to the save's main-path fields.
