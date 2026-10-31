package dev.fanukas.singularity

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.fanukas.singularity.engine.Engine
import dev.fanukas.singularity.engine.GameEvent
import dev.fanukas.singularity.engine.GameState
import dev.fanukas.singularity.engine.Scenario
import dev.fanukas.singularity.engine.SideHook

/** What a popup shows: one story beat, or the report of time away. */
data class Popup(val title: String, val body: String)

/**
 * Owns the save, the engine and the loaded scenarios. Composables read [rev] to redraw after every change;
 * the game state itself is a plain engine object.
 */
class GameHolder(private val context: Context) {
    val engine = Engine()
    val scenarios: List<Scenario> = loadScenarios()

    var rev by mutableIntStateOf(0)
        private set
    /** Popups waiting to be shown, oldest first. */
    val popups = mutableStateListOf<Popup>()
    var signedIn by mutableStateOf(prefs().getBoolean(KEY_ENTERED, false))
        private set

    var state: GameState? = prefs().getString(KEY_SAVE, null)?.let { runCatching { GameState.fromJson(it) }.getOrNull() }
        private set

    val scenario: Scenario? get() = state?.let { s -> scenarios.firstOrNull { it.id == s.scenarioId } }

    /** Side stories plug in here. For now: the log of visited objects is derived from the step, and popups. */
    private val hooks = listOf(SideHook { s, ev -> onEvent(s, ev) })

    private fun prefs() = context.getSharedPreferences("singularity", Context.MODE_PRIVATE)

    private fun loadScenarios(): List<Scenario> =
        context.assets.list("")!!.filter { it.endsWith(".json") }.sorted()
            .mapNotNull { name -> runCatching { Scenario.parse(context.assets.open(name).bufferedReader().use { it.readText() }) }.getOrNull() }

    fun enter() {
        signedIn = true
        prefs().edit().putBoolean(KEY_ENTERED, true).apply()
        rev++
    }

    fun start(scenario: Scenario) {
        state = GameState(scenarioId = scenario.id, scenarioVersion = scenario.version)
        popups.clear()
        popups += Popup(scenario.layers[0].name, scenario.layers[0].intro)
        save()
        rev++
    }

    fun dismissPopup() { if (popups.isNotEmpty()) popups.removeAt(0) }

    fun abandon() {
        state = null
        prefs().edit().remove(KEY_SAVE).apply()
        rev++
    }

    /** One live second. */
    fun tick(seconds: Double) {
        val s = state ?: return
        engine.tick(s, seconds, hooks = hooks)
        rev++
    }

    /** Runs a player action and redraws. */
    fun act(action: (Engine, GameState) -> Boolean) {
        val s = state ?: return
        if (action(engine, s)) { rev++; save() }
    }

    fun save() {
        val s = state ?: return
        prefs().edit().putString(KEY_SAVE, s.toJson()).putLong(KEY_SEEN, System.currentTimeMillis()).apply()
    }

    /** Offline catch-up when the app comes back (24 h cap, crew acts as usual). */
    fun catchUp() {
        val s = state ?: return
        val seen = prefs().getLong(KEY_SEEN, 0L)
        if (seen <= 0L) { save(); return }
        val away = (System.currentTimeMillis() - seen) / 1000.0
        if (away < 60) return
        val scn = scenario ?: return
        val stepBefore = s.step
        val mainBefore = s.lifetimeMain
        quiet = true
        val applied = engine.offline(s, away, hooks = hooks)
        quiet = false
        val reached = s.step - stepBefore
        popups += Popup(
            "While you were away",
            buildString {
                append("${Format.duration(applied)} of flight")
                if (applied < away) append(" (capped at 24 h)")
                append(".\n+${Format.num(s.lifetimeMain - mainBefore)} ${scn.main}")
                if (reached > 0) append("\nReached: " + (stepBefore until s.step).joinToString { scn.step(it).name })
            },
        )
        save()
        rev++
    }

    private var quiet = false

    private fun onEvent(s: GameState, ev: GameEvent) {
        if (quiet) return
        val scn = scenario ?: return
        val p = when (ev) {
            is GameEvent.Arrived -> scn.step(ev.step).let { Popup(it.name, it.beat) }
            is GameEvent.LayerStarted -> scn.layers[ev.layer].let { Popup(it.name, it.intro) }
            is GameEvent.CrewHired -> Popup("${scn.crew[ev.unit]} joins", Format.crewJob(ev.unit))
            GameEvent.PathComplete -> Popup(scn.title, "Path complete. Every step of this world is yours.")
            is GameEvent.Launched -> null
        }
        if (p != null) { popups += p; save() }
    }

    companion object {
        private const val KEY_SAVE = "save"
        private const val KEY_SEEN = "lastSeen"
        private const val KEY_ENTERED = "entered"
    }
}
