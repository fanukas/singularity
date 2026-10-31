package dev.fanukas.singularity.engine

import kotlin.math.abs

/** Fiction ladder (spec section 4): which kind of science carries a mechanic. */
enum class Rung(val label: String, val maxSpeed: Double) {
    REAL("Known physics", 200_000.0),           // m/s: proven flight hardware (Parker Solar Probe ~190 km/s)
    THEORY("Leading theory", 0.9 * Units.C),    // fusion torches, light sails, antimatter
    HYPOTHESIS("Plausible hypothesis", Double.POSITIVE_INFINITY), // warp bubbles, wormholes
    ANCIENT("Ancient civilization", Double.POSITIVE_INFINITY);

    companion object { fun of(n: Int) = entries[(n - 1).coerceIn(0, 3)] }
}

object Units {
    const val C = 299_792_458.0
    const val KM = 1_000.0
    const val AU = 1.495978707e11
    const val LY = 9.4607304725808e15
    const val PC = 3.0856775814913673e16

    fun toMeters(value: Double, unit: String): Double = value * when (unit) {
        "m" -> 1.0; "km" -> KM; "AU" -> AU; "ly" -> LY; "kly" -> 1e3 * LY; "Mly" -> 1e6 * LY; "Gly" -> 1e9 * LY
        "pc" -> PC; "kpc" -> 1e3 * PC; "Mpc" -> 1e6 * PC
        else -> error("unknown unit '$unit'")
    }

    /** Human text for a distance, picking the unit that reads best. */
    fun formatDistance(m: Double): String = when {
        m < 1e7 -> "%,.0f km".format(m / KM)
        m < 0.05 * AU -> "%,.0f thousand km".format(m / 1e6)
        m < 0.1 * LY -> "%,.2f AU".format(m / AU)
        m < 1e4 * LY -> "%,.2f ly".format(m / LY)
        m < 1e9 * LY -> "%,.2f million ly".format(m / (1e6 * LY))
        else -> "%,.2f billion ly".format(m / (1e9 * LY))
    }

    fun formatSpeed(mps: Double): String = when {
        mps < 1e5 -> "%,.1f km/s".format(mps / KM)
        mps < 0.01 * C -> "%,.0f km/s".format(mps / KM)
        mps < C -> "%.3fc".format(mps / C)
        else -> "%,.3g × light (warp)".format(mps / C)
    }
}

/** One real object at a step (spec section 5). */
class StepObject(
    val name: String,
    val kind: String,
    /** Straight-line distance from the path's origin, in meters. */
    val distance: Double,
    /** Length of the leg that ends here when it isn't the change in [distance] (moves on a surface or around a planet). */
    val leg: Double?,
    /** Engine rung for the drive used on this leg. */
    val rung: Rung,
    val catalogued: Boolean,
    val beat: String,
    val fact: String,
)

class Layer(
    val name: String,
    val drive: String,
    val intro: String,
    val industry: String,
    val research: List<String>,
    val steps: List<StepObject>,
)

/**
 * A scenario (a path) is pure data: names, objects, beats. The engine reads it only for display and validation;
 * gameplay numbers come from [Skeleton] and [LaunchTable] (spec section 3).
 */
class Scenario(
    val id: String,
    val version: Int,
    val title: String,
    val tagline: String,
    val summary: String,
    val origin: String,
    val main: String,
    val slow: String,
    val records: String,
    val crew: List<String>,
    val layers: List<Layer>,
) {
    val steps: List<StepObject> get() = layers.flatMap { it.steps }
    fun step(k: Int) = steps[k.coerceIn(0, Skeleton.STEPS - 1)]
    fun layer(k: Int) = layers[Skeleton.layerOf(k)]

    /** Length of the leg that ends at step [k], in meters. */
    fun legLength(k: Int): Double {
        val all = steps
        all[k].leg?.let { return it }
        val from = if (k == 0) 0.0 else all[k - 1].distance
        return abs(all[k].distance - from)
    }

    /** Fastest the ship moves on the leg to [k]: with the drive maxed for that layer. */
    fun topSpeed(k: Int): Double {
        val t = Skeleton.travelBase(k) / Skeleton.driveMult(Skeleton.driveCap(Skeleton.layerOf(k)))
        return legLength(k) / t
    }

    companion object {
        fun parse(text: String): Scenario {
            val m = Json.parse(text).obj()
            val names = m["names"].obj()
            return Scenario(
                id = m.str("id"), version = m.int("version"), title = m.str("title"), tagline = m.str("tagline"),
                summary = m.str("summary"), origin = m.strOr("origin", "Earth"),
                main = names.str("main"), slow = names.str("slow"), records = names.str("records"),
                crew = names["crew"].list().map { it as String },
                layers = m["layers"].list().map { l ->
                    val lo = l.obj()
                    Layer(
                        name = lo.str("name"), drive = lo.str("drive"), intro = lo.str("intro"),
                        industry = lo.str("industry"),
                        research = lo["research"].list().map { it as String },
                        steps = lo["steps"].list().map { st ->
                            val so = st.obj()
                            val d = so["distance"].list()
                            val leg = so["leg"]?.list()
                            StepObject(
                                name = so.str("name"), kind = so.str("kind"),
                                distance = Units.toMeters((d[0] as Number).toDouble(), d[1] as String),
                                leg = leg?.let { Units.toMeters((it[0] as Number).toDouble(), it[1] as String) },
                                rung = Rung.of(so.int("rung")), catalogued = so.bool("catalogued", true),
                                beat = so.strOr("beat", ""), fact = so.strOr("fact", ""),
                            )
                        },
                    )
                },
            )
        }
    }
}

/** CI's checks on a scenario file (spec section 8). Returns the problems; empty means shippable. */
object Validator {
    fun check(s: Scenario): List<String> {
        val p = ArrayList<String>()
        if (s.layers.size != Skeleton.LAYERS) p += "${s.id}: ${s.layers.size} layers, the skeleton has ${Skeleton.LAYERS}"
        s.layers.forEachIndexed { i, l ->
            if (l.steps.size != Skeleton.STEPS_PER_LAYER) p += "${s.id}: layer ${i + 1} has ${l.steps.size} steps, needs ${Skeleton.STEPS_PER_LAYER}"
            if (l.research.size != Skeleton.RESEARCH_PER_LAYER) p += "${s.id}: layer ${i + 1} names ${l.research.size} research nodes, needs ${Skeleton.RESEARCH_PER_LAYER}"
        }
        if (s.crew.size != Skeleton.CREW_STEP.size) p += "${s.id}: ${s.crew.size} crew names, needs ${Skeleton.CREW_STEP.size}"
        if (p.isNotEmpty()) return p
        val all = s.steps
        val seen = HashSet<String>()
        for (k in all.indices) {
            val o = all[k]
            val where = "${s.id} step ${k + 1} (${o.name})"
            if (o.name.isBlank() || o.kind.isBlank()) p += "$where: empty object"
            if (!seen.add(o.name)) p += "$where: object used twice"
            if (!(o.distance > 0)) p += "$where: distance must be above zero"
            if (o.beat.isBlank()) p += "$where: no story beat"
            if (!o.catalogued && o.fact.isBlank()) p += "$where: uncatalogued object needs a fact saying why its kind exists there"
            val top = s.topSpeed(k)
            if (top > o.rung.maxSpeed) p += "$where: top speed ${Units.formatSpeed(top)} is above ${o.rung.label} (${Units.formatSpeed(o.rung.maxSpeed)}); raise the rung or move the object"
        }
        return p
    }
}
