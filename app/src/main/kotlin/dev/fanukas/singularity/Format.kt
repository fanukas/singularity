package dev.fanukas.singularity

import dev.fanukas.singularity.engine.Skeleton
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Text for numbers and times. Plain Kotlin, no Android, so the sandbox can test it. */
object Format {
    private val SUFFIX = listOf("", "K", "M", "B", "T", "Qa", "Qi")

    fun num(v: Double): String {
        if (v.isNaN()) return "?"
        if (v.isInfinite()) return "∞"
        val a = kotlin.math.abs(v)
        if (a < 1000) return if (a < 10 && a != floor(a)) "%.1f".format(v) else "%.0f".format(v)
        val e = floor(log10(a)).toInt()
        val group = e / 3
        if (group < SUFFIX.size) return "%.2f%s".format(v / 10.0.pow(group * 3), SUFFIX[group])
        return "%.2fe%d".format(v / 10.0.pow(e), e)
    }

    fun duration(seconds: Double): String {
        val s = seconds.coerceAtLeast(0.0)
        return when {
            s < 60 -> "${s.toInt()} s"
            s < 3600 -> "${(s / 60).toInt()} min ${(s % 60).toInt()} s"
            s < Skeleton.DAY -> "${(s / 3600).toInt()} h ${((s % 3600) / 60).toInt()} min"
            else -> "${(s / Skeleton.DAY).toInt()} d ${((s % Skeleton.DAY) / 3600).toInt()} h"
        }
    }

    fun crewJob(unit: Int): String = when (unit) {
        Skeleton.NAVIGATOR -> "Launches the next leg as soon as the fuel is saved, and buys drive refits."
        Skeleton.ENGINEER -> "Buys industry with whatever is left after the launch fund."
        else -> "Buys research as soon as it can be paid."
    }

    fun researchEffect(node: Int): String = when (Skeleton.researchEffect(node)) {
        Skeleton.Effect.INDUSTRY_X2 -> "Industry ×2"
        Skeleton.Effect.INDUSTRY_X3 -> "Industry ×3"
        Skeleton.Effect.DATA_X2 -> "Slow currency ×2"
    }
}
