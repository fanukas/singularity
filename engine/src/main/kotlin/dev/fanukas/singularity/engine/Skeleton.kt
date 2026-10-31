package dev.fanukas.singularity.engine

import kotlin.math.pow

/**
 * The fixed skeleton every scenario shares (docs/engine-spec.md, section 1). Nothing here knows a place or a name.
 * Changing a number here changes every scenario at once; the pacing solver then re-derives the launch costs.
 */
object Skeleton {
    const val LAYERS = 6
    const val STEPS_PER_LAYER = 12
    const val STEPS = LAYERS * STEPS_PER_LAYER

    const val DAY = 86_400.0
    const val MIN = 60.0

    fun layerOf(step: Int): Int = (step / STEPS_PER_LAYER).coerceIn(0, LAYERS - 1)

    // ---- Reference pacing curve: when the reference player arrives at each step (seconds of real time). ----

    /** Day each layer ends on the reference curve (about 2 years in total). */
    val LAYER_END_DAYS = doubleArrayOf(60.0, 180.0, 300.0, 420.0, 600.0, 730.0)

    /** Layer 1 starts with a guided first hour, then stretches out; the minutes/days of each of its 12 steps. */
    private val FIRST_LAYER = doubleArrayOf(
        3 * MIN, 5 * MIN, 8 * MIN, 12 * MIN, 18 * MIN, 25 * MIN,
        0.5 * DAY, 1.5 * DAY, 4 * DAY, 8 * DAY, 16 * DAY, 0.0, // last one fills the layer up to its end day
    )

    /** Duration of every step on the reference curve, in seconds. */
    val STEP_DURATION: DoubleArray = DoubleArray(STEPS).also { d ->
        var sum = 0.0
        for (j in 0 until STEPS_PER_LAYER - 1) { d[j] = FIRST_LAYER[j]; sum += d[j] }
        d[STEPS_PER_LAYER - 1] = LAYER_END_DAYS[0] * DAY - sum
        for (l in 1 until LAYERS) {
            val span = (LAYER_END_DAYS[l] - LAYER_END_DAYS[l - 1]) * DAY
            val w = DoubleArray(STEPS_PER_LAYER) { 1.08.pow(it) }
            val ws = w.sum()
            for (j in 0 until STEPS_PER_LAYER) d[l * STEPS_PER_LAYER + j] = span * w[j] / ws
        }
    }

    /** Arrival time of each step on the reference curve (cumulative). */
    val ARRIVAL: DoubleArray = DoubleArray(STEPS).also { a ->
        var t = 0.0
        for (k in 0 until STEPS) { t += STEP_DURATION[k]; a[k] = t }
    }

    // ---- System 1: Drive (capped per layer, refit raises the cap at each new layer). ----
    const val DRIVE_PER_LAYER = 4
    const val DRIVE_MULT = 1.35
    fun driveCap(layer: Int) = DRIVE_PER_LAYER * (layer + 1)
    /** Drive refits are paid in the slow currency, so drive progress doesn't compete with saving for launches. */
    fun driveCost(level: Int) = 2.0 * 1.9.pow(level)
    fun driveMult(level: Int) = DRIVE_MULT.pow(level)

    /** Share of a step spent travelling when the drive is maxed for that layer; the rest is saving up the launch. */
    const val TRAVEL_SHARE = 0.3

    /** Travel time of the leg to [step] at drive level 0; the actual time is this divided by the drive multiplier. */
    fun travelBase(step: Int) = TRAVEL_SHARE * STEP_DURATION[step] * driveMult(driveCap(layerOf(step)))

    // ---- System 2: Industry (one endless track per layer, the main currency). ----
    const val TIERS = LAYERS
    const val INDUSTRY_GROWTH = 1.15
    private const val TIER_SCALE = 1e5
    fun tierUnlocked(tier: Int, step: Int) = layerOf(step) >= tier
    fun industryCost(tier: Int, level: Int) = 10.0 * TIER_SCALE.pow(tier) * INDUSTRY_GROWTH.pow(level)
    fun industryBase(tier: Int) = 1.0 * TIER_SCALE.pow(tier)
    /** Every 25 levels of a tier doubles that tier's output. */
    const val MILESTONE_LEVELS = 25
    fun milestoneMult(level: Int) = 2.0.pow(level / MILESTONE_LEVELS)
    const val START_MAIN = 20.0

    // ---- System 3: Research (finite, six nodes per layer, paid in the slow currency). ----
    const val RESEARCH_PER_LAYER = 6
    const val RESEARCH = LAYERS * RESEARCH_PER_LAYER
    fun researchUnlocked(node: Int, step: Int) = node / RESEARCH_PER_LAYER <= layerOf(step)
    /** Research is priced against the slow currency each layer earns, so its nodes are bought across the layer. */
    private val RESEARCH_BASE = doubleArrayOf(5.0, 7e4, 2e5, 5.5e5, 2e6, 3.5e6)
    private val RESEARCH_GROWTH = doubleArrayOf(8.0, 1.6, 1.6, 1.6, 1.6, 1.6)
    fun researchCost(node: Int): Double {
        val l = node / RESEARCH_PER_LAYER
        return RESEARCH_BASE[l] * RESEARCH_GROWTH[l].pow(node % RESEARCH_PER_LAYER)
    }

    /** What each node in a layer does, by its position in the layer. */
    enum class Effect { INDUSTRY_X2, DATA_X2, INDUSTRY_X3 }
    fun researchEffect(node: Int): Effect = when (node % RESEARCH_PER_LAYER) {
        2 -> Effect.DATA_X2
        5 -> Effect.INDUSTRY_X3
        else -> Effect.INDUSTRY_X2
    }

    /** Slow currency per second before research: grows with records. */
    fun slowBase(records: Int) = 0.002 * (1 + records)

    // ---- System 4: Crew (automation, unlocked by the guided first hour). ----
    /** Step reached that hires each crew unit: navigator (launch + drive), engineer (industry), scientist (research). */
    val CREW_STEP = intArrayOf(2, 3, 5)
    const val NAVIGATOR = 0
    const val ENGINEER = 1
    const val SCIENTIST = 2

    // ---- System 5: Records (permanent; one per step reached). ----
    const val RECORD_BONUS = 0.25
    fun recordMult(records: Int) = 1.0 + RECORD_BONUS * records

    /** Offline progress is capped and simulated in one-minute steps. */
    const val OFFLINE_CAP = 24 * 3600.0
    const val OFFLINE_TICK = 60.0
}
