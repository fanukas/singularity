@file:JvmName("SolveTable")

package dev.fanukas.singularity.engine

import java.io.File

/**
 * Regenerates LaunchTable.kt after a skeleton change: `gradle :engine:solveTable`.
 * Prints the pacing report so the change can be checked against the reference curve.
 */
fun main(args: Array<String>) {
    val out = File(args.firstOrNull() ?: "engine/src/main/kotlin/dev/fanukas/singularity/engine/LaunchTable.kt")
    val costs = Pacing.solve { println(it) }
    out.writeText(Pacing.tableSource(costs))
    println(Pacing.report(Pacing.replay(Engine(costs))))
    println("Wrote ${out.path}")
}
