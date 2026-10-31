package dev.fanukas.singularity.engine

import java.io.File

/** Plain checks shared by the JUnit tests (CI) and the sandbox runner. Each throws on failure and returns a report line. */
object EngineChecks {

    fun scenarioDir(): File = listOf(File("scenarios"), File("../scenarios")).first { it.isDirectory }

    fun scenarios(): List<Scenario> = scenarioDir().listFiles { f -> f.name.endsWith(".json") }!!.sortedBy { it.name }
        .map { Scenario.parse(it.readText()) }

    fun pacingMatchesReferenceCurve(): String {
        val arrivals = Pacing.replay()
        val miss = Pacing.worstMiss(arrivals)
        check(miss < 0.02) { "reference player misses the curve by ${"%.1f".format(miss * 100)}%\n" + Pacing.report(arrivals) }
        return Pacing.report(arrivals)
    }

    fun tableIsSolvedFromSkeleton(): String {
        val solved = Pacing.solve()
        for (k in solved.indices) check(solved[k] == LaunchTable.COSTS[k]) {
            "LaunchTable.kt is stale at step ${k + 1}: regenerate it with the solver (README)"
        }
        return "PACING table: matches the solver"
    }

    /** Offline catch-up uses one-minute ticks; after the first hour it must keep the same pace. */
    fun offlineTicksKeepPace(): String {
        val arrivals = Pacing.replay(Engine(), Skeleton.OFFLINE_TICK)
        var worst = 0.0
        for (k in 6 until 36) worst = maxOf(worst, kotlin.math.abs(arrivals[k] - Skeleton.ARRIVAL[k]) / Skeleton.ARRIVAL[k])
        check(worst < 0.03) { "one-minute ticks drift ${"%.1f".format(worst * 100)}% from the curve" }
        return "PACING offline ticks: worst drift %.2f%% (steps 7-36)".format(worst * 100)
    }

    fun everyScenarioIsValid(): String {
        val all = scenarios()
        check(all.size >= 2) { "expected at least two scenarios, found ${all.size}" }
        val problems = all.flatMap { Validator.check(it) }
        check(problems.isEmpty()) { problems.joinToString("\n") }
        check(all.map { it.id }.toSet().size == all.size) { "two scenarios share an id" }
        return "SCENARIOS valid: " + all.joinToString { it.id }
    }

    fun saveRoundTrips(): String {
        val e = Engine()
        val s = GameState(scenarioId = "earth-mars")
        repeat(5000) { e.tick(s, 1.0, everyoneAutomates = true) }
        val back = GameState.fromJson(s.toJson())
        check(back.toJson() == s.toJson()) { "save changed after a round trip" }
        check(back.step == s.step && back.main == s.main && back.industry.contentEquals(s.industry)) { "fields lost" }
        return "SAVE round trip ok at step ${s.step}"
    }

    fun offlineIsCapped(): String {
        val e = Engine()
        val s = GameState()
        val applied = e.offline(s, 10 * Skeleton.DAY)
        check(applied == Skeleton.OFFLINE_CAP) { "offline applied $applied s" }
        check(s.time == Skeleton.OFFLINE_CAP) { "game time ${s.time}" }
        return "OFFLINE cap ok"
    }

    /** Before the crew is hired nothing is bought for the player; offline time then only produces. */
    fun noCrewNoAutoBuying(): String {
        val e = Engine()
        val s = GameState()
        e.buyIndustry(s, 0)
        val levels = s.industry[0]
        e.offline(s, 3600.0)
        check(s.industry[0] == levels) { "something bought industry without a crew" }
        check(s.main > 0) { "no production" }
        return "CREW gates automation ok"
    }

    /** Side stories see every event, and the same run gives the same result with or without them (spec section 6). */
    fun sideHooksNeverChangeTheMainPath(): String {
        val seen = ArrayList<GameEvent>()
        val hook = SideHook { _, ev -> seen += ev }
        val a = GameState(); val b = GameState()
        val e = Engine()
        repeat(4 * 3600) { e.tick(a, 1.0, hooks = listOf(hook), everyoneAutomates = true); e.tick(b, 1.0, everyoneAutomates = true) }
        check(a.toJson() == b.toJson()) { "a side hook changed the main path" }
        check(seen.count { it is GameEvent.Arrived } == a.step) { "hook missed arrivals" }
        return "HOOKS saw ${seen.size} events"
    }

    fun all(): List<Pair<String, () -> String>> = listOf(
        "pacing" to ::pacingMatchesReferenceCurve,
        "table" to ::tableIsSolvedFromSkeleton,
        "offline ticks" to ::offlineTicksKeepPace,
        "scenarios" to ::everyScenarioIsValid,
        "save" to ::saveRoundTrips,
        "offline cap" to ::offlineIsCapped,
        "crew" to ::noCrewNoAutoBuying,
        "hooks" to ::sideHooksNeverChangeTheMainPath,
    )
}
