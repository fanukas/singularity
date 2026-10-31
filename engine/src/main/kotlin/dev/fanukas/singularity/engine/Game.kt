package dev.fanukas.singularity.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Everything a save holds. No scenario-specific fields: the scenario id only picks the names, objects and art
 * (docs/engine-spec.md, section 8).
 */
class GameState(
    var scenarioId: String = "",
    var scenarioVersion: Int = 1,
    var time: Double = 0.0,
    var main: Double = Skeleton.START_MAIN,
    var lifetimeMain: Double = 0.0,
    var slow: Double = 0.0,
    /** Index of the next step to reach; [Skeleton.STEPS] when the path is complete. */
    var step: Int = 0,
    var traveling: Boolean = false,
    /** Share of the current leg already flown, 0..1. */
    var progress: Double = 0.0,
    val industry: IntArray = IntArray(Skeleton.TIERS),
    var drive: Int = 0,
    val research: BooleanArray = BooleanArray(Skeleton.RESEARCH),
) {
    val records get() = step
    val finished get() = step >= Skeleton.STEPS
    val layer get() = Skeleton.layerOf(step)

    fun copy() = GameState(
        scenarioId, scenarioVersion, time, main, lifetimeMain, slow, step, traveling, progress,
        industry.copyOf(), drive, research.copyOf(),
    )

    fun crewHired(unit: Int) = step >= Skeleton.CREW_STEP[unit]

    fun toJson(): String = Json.write(
        mapOf(
            "v" to 1, "scenario" to scenarioId, "scenarioVersion" to scenarioVersion,
            "time" to time, "main" to main, "lifetimeMain" to lifetimeMain, "slow" to slow,
            "step" to step, "traveling" to traveling, "progress" to progress,
            "industry" to industry.toList(), "drive" to drive, "research" to research.toList(),
        )
    )

    companion object {
        fun fromJson(text: String): GameState {
            val m = Json.parse(text).obj()
            val s = GameState(
                scenarioId = m.str("scenario"), scenarioVersion = m.int("scenarioVersion"),
                time = m.num("time"), main = m.num("main"), lifetimeMain = m.num("lifetimeMain"), slow = m.num("slow"),
                step = m.int("step"), traveling = m.bool("traveling"), progress = m.num("progress"), drive = m.int("drive"),
            )
            m["industry"].list().forEachIndexed { i, v -> if (i < s.industry.size) s.industry[i] = (v as Number).toInt() }
            m["research"].list().forEachIndexed { i, v -> if (i < s.research.size) s.research[i] = v as Boolean }
            return s
        }
    }
}

/** Something that happened during a tick, for the log, popups and side-story hooks. */
sealed class GameEvent {
    data class Launched(val step: Int) : GameEvent()
    data class Arrived(val step: Int) : GameEvent()
    data class LayerStarted(val layer: Int) : GameEvent()
    data class CrewHired(val unit: Int) : GameEvent()
    data object PathComplete : GameEvent()
}

/** Side stories listen here; they can collect and rank, never change the main path (spec section 6). */
fun interface SideHook { fun onEvent(state: GameState, event: GameEvent) }

/**
 * The rules. Pure functions on [GameState]; the same code runs live, offline, in the pacing solver and on a server.
 * [launchCosts] defaults to the solved table; the solver passes its own while it searches.
 */
class Engine(val launchCosts: DoubleArray = LaunchTable.COSTS) {

    fun launchCost(step: Int) = if (step < Skeleton.STEPS) launchCosts[step] else 0.0

    fun industryMult(s: GameState): Double {
        var m = Skeleton.recordMult(s.records)
        for (i in 0 until Skeleton.RESEARCH) if (s.research[i]) when (Skeleton.researchEffect(i)) {
            Skeleton.Effect.INDUSTRY_X2 -> m *= 2.0
            Skeleton.Effect.INDUSTRY_X3 -> m *= 3.0
            Skeleton.Effect.DATA_X2 -> {}
        }
        return m
    }

    fun mainRate(s: GameState): Double {
        var base = 0.0
        for (t in 0 until Skeleton.TIERS) base += Skeleton.industryBase(t) * s.industry[t] * Skeleton.milestoneMult(s.industry[t])
        return base * industryMult(s)
    }

    fun slowRate(s: GameState): Double {
        var m = Skeleton.slowBase(s.records)
        for (i in 0 until Skeleton.RESEARCH) if (s.research[i] && Skeleton.researchEffect(i) == Skeleton.Effect.DATA_X2) m *= 2.0
        return m
    }

    /** Seconds the current (or next) leg takes from start to end at the current drive. */
    fun legTime(s: GameState) = if (s.finished) 0.0 else Skeleton.travelBase(s.step) / Skeleton.driveMult(s.drive)

    fun travelTimeLeft(s: GameState) = if (!s.traveling) 0.0 else legTime(s) * (1 - s.progress)

    // ---- Actions (the player's taps; crew automation calls the same ones). ----

    fun canLaunch(s: GameState) = !s.finished && !s.traveling && s.main >= launchCost(s.step)
    fun launch(s: GameState, events: MutableList<GameEvent>? = null): Boolean {
        if (!canLaunch(s)) return false
        s.main -= launchCost(s.step)
        s.traveling = true
        s.progress = 0.0
        events?.add(GameEvent.Launched(s.step))
        return true
    }

    fun canBuyDrive(s: GameState) = s.drive < Skeleton.driveCap(s.layer) && s.slow >= Skeleton.driveCost(s.drive)
    fun buyDrive(s: GameState): Boolean {
        if (!canBuyDrive(s)) return false
        s.slow -= Skeleton.driveCost(s.drive); s.drive++
        return true
    }

    fun canBuyIndustry(s: GameState, tier: Int, reserve: Double = 0.0) =
        Skeleton.tierUnlocked(tier, s.step) && s.main - reserve >= Skeleton.industryCost(tier, s.industry[tier])
    fun buyIndustry(s: GameState, tier: Int, reserve: Double = 0.0): Boolean {
        if (!canBuyIndustry(s, tier, reserve)) return false
        s.main -= Skeleton.industryCost(tier, s.industry[tier]); s.industry[tier]++
        return true
    }

    fun canBuyResearch(s: GameState, node: Int) =
        !s.research[node] && Skeleton.researchUnlocked(node, s.step) && s.slow >= Skeleton.researchCost(node)
    fun buyResearch(s: GameState, node: Int): Boolean {
        if (!canBuyResearch(s, node)) return false
        s.slow -= Skeleton.researchCost(node); s.research[node] = true
        return true
    }

    // ---- Crew automation: the same policy the pacing solver's reference player uses. ----

    /** What the crew does each tick. While docked, the navigator saves the launch cost before any industry is bought. */
    fun automate(s: GameState, events: MutableList<GameEvent>? = null, everyone: Boolean = false) {
        val nav = everyone || s.crewHired(Skeleton.NAVIGATOR)
        val eng = everyone || s.crewHired(Skeleton.ENGINEER)
        val sci = everyone || s.crewHired(Skeleton.SCIENTIST)
        if (nav) launch(s, events)
        val reserve = if (nav && !s.traveling && !s.finished) launchCost(s.step) else 0.0
        if (nav) while (buyDrive(s)) { /* the drive comes first: travel time is the pacing */ }
        if (sci) {
            var bought = true
            while (bought) {
                bought = false
                var best = -1
                for (i in 0 until Skeleton.RESEARCH) if (canBuyResearch(s, i) && (best < 0 || Skeleton.researchCost(i) < Skeleton.researchCost(best))) best = i
                if (best >= 0) bought = buyResearch(s, best)
            }
        }
        if (eng) {
            // Buy the best production per cost until the money above the reserve runs out.
            while (true) {
                var best = -1
                var bestValue = 0.0
                for (t in 0 until Skeleton.TIERS) if (canBuyIndustry(s, t, reserve)) {
                    val v = Skeleton.industryBase(t) * Skeleton.milestoneMult(s.industry[t] + 1) / Skeleton.industryCost(t, s.industry[t])
                    if (v > bestValue) { bestValue = v; best = t }
                }
                if (best < 0 || !buyIndustry(s, best, reserve)) break
            }
        }
    }

    // ---- Time ----

    /**
     * Advances [dt] seconds. Production and travel use the rates at the start of the tick; the crew acts at the end.
     * Ticks of one second (live) or one minute (offline) give the same pacing within the solver's tolerance.
     */
    fun tick(s: GameState, dt: Double, events: MutableList<GameEvent>? = null, hooks: List<SideHook> = emptyList(), everyoneAutomates: Boolean = false) {
        if (dt <= 0) return
        val gained = mainRate(s) * dt
        s.main += gained
        s.lifetimeMain += gained
        s.slow += slowRate(s) * dt
        s.time += dt
        val local = if (hooks.isEmpty()) events else (events ?: ArrayList())
        val before = local?.size ?: 0
        if (s.traveling && !s.finished) {
            s.progress += dt / legTime(s)
            if (s.progress >= 1.0) arrive(s, local)
        }
        automate(s, local, everyoneAutomates)
        if (local != null && hooks.isNotEmpty()) for (i in before until local.size) for (h in hooks) h.onEvent(s, local[i])
    }

    private fun arrive(s: GameState, events: MutableList<GameEvent>?) {
        val oldLayer = s.layer
        val oldCrew = BooleanArray(Skeleton.CREW_STEP.size) { s.crewHired(it) }
        s.traveling = false
        s.progress = 0.0
        events?.add(GameEvent.Arrived(s.step))
        s.step++
        for (u in Skeleton.CREW_STEP.indices) if (!oldCrew[u] && s.crewHired(u)) events?.add(GameEvent.CrewHired(u))
        if (s.finished) events?.add(GameEvent.PathComplete)
        else if (s.layer != oldLayer) events?.add(GameEvent.LayerStarted(s.layer))
    }

    /** Offline catch-up: capped, simulated in one-minute steps, crew acts as usual. Returns the seconds applied. */
    fun offline(s: GameState, seconds: Double, events: MutableList<GameEvent>? = null, hooks: List<SideHook> = emptyList()): Double {
        val total = min(max(seconds, 0.0), Skeleton.OFFLINE_CAP)
        var left = total
        while (left > 0) {
            val dt = min(Skeleton.OFFLINE_TICK, left)
            tick(s, dt, events, hooks)
            left -= dt
        }
        return total
    }

    /** One number for shared leaderboards: steps reached plus the share of the current leg (spec section 7). */
    fun progressScore(s: GameState) = s.step + if (s.traveling) s.progress.coerceIn(0.0, 0.999) else 0.0
}
