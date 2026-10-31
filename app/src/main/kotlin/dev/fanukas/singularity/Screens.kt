package dev.fanukas.singularity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fanukas.singularity.engine.Engine
import dev.fanukas.singularity.engine.GameState
import dev.fanukas.singularity.engine.Rung
import dev.fanukas.singularity.engine.Scenario
import dev.fanukas.singularity.engine.Skeleton
import dev.fanukas.singularity.engine.Units

@Composable
fun App(holder: GameHolder) {
    holder.rev
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val state = holder.state
        val scenario = holder.scenario
        when {
            !holder.signedIn -> SignInScreen(holder)
            state == null || scenario == null -> PickerScreen(holder)
            else -> GameScreen(holder, state, scenario)
        }
    }
    holder.popups.firstOrNull()?.let { p ->
        AlertDialog(
            onDismissRequest = { holder.dismissPopup() },
            confirmButton = { TextButton(onClick = { holder.dismissPopup() }) { Text("Continue") } },
            title = { Text(p.title) },
            text = { Text(p.body) },
        )
    }
}

@Composable
private fun SignInScreen(holder: GameHolder) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Singularity", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("Real distances. Real time. Pick a world and fly it.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        // Google sign-in needs the Firebase project and server (docs/server-api.md); until then it is shown, not wired.
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Sign in with Google (arrives with the server)")
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { holder.enter() }, modifier = Modifier.fillMaxWidth()) { Text("Play on this phone") }
    }
}

@Composable
private fun PickerScreen(holder: GameHolder) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Choose your world", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Every world has the same ${Skeleton.STEPS} steps and the same pace, so all players share one leaderboard.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(holder.scenarios) { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    WideSlot(slotName("scn", s.id, "banner"), SlotShape.BANNER, s.title)
                    Spacer(Modifier.height(8.dp))
                    Text(s.title, style = MaterialTheme.typography.titleLarge)
                    Text(s.tagline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(s.summary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Science: " + rungMix(s), style = MaterialTheme.typography.labelMedium)
                    Text("Goal: ${s.steps.last().name}, ${Units.formatDistance(s.steps.last().distance)} away · about 2 years", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { holder.start(s) }, modifier = Modifier.fillMaxWidth()) { Text("Fly this world") }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun rungMix(s: Scenario): String =
    Rung.entries.mapNotNull { r -> s.steps.count { it.rung == r }.takeIf { it > 0 }?.let { "${r.label} $it" } }.joinToString(" · ")

private val TABS = listOf("Journey", "Industry", "Research", "Crew", "Log", "Board")

@Composable
private fun GameScreen(holder: GameHolder, s: GameState, scn: Scenario) {
    var tab by remember { mutableIntStateOf(0) }
    holder.rev // the game state is a plain object: reading rev redraws on every tick
    val e = holder.engine
    Column(Modifier.fillMaxSize()) {
        // Currencies, always visible.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("${Format.num(s.main)} ${scn.main}", style = MaterialTheme.typography.titleMedium)
                Text("+${Format.num(e.mainRate(s))}/s", style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Format.num(s.slow)} ${scn.slow}", style = MaterialTheme.typography.titleMedium)
                Text("+${Format.num(e.slowRate(s) * 3600)}/h · ${s.records} ${scn.records}", style = MaterialTheme.typography.labelSmall)
            }
        }
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
            TABS.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Spacer(Modifier.height(4.dp)) }
            when (tab) {
                0 -> item { JourneyTab(holder, e, s, scn) }
                1 -> items(Skeleton.TIERS) { t -> IndustryRow(holder, e, s, scn, t) }
                2 -> items(Skeleton.RESEARCH) { n -> ResearchRow(holder, e, s, scn, n) }
                3 -> items(Skeleton.CREW_STEP.size) { u -> CrewRow(holder, s, scn, u) }
                4 -> items(s.step) { k -> LogRow(scn, s.step - 1 - k) }
                else -> item { BoardTab(holder, e, s, scn) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun JourneyTab(holder: GameHolder, e: Engine, s: GameState, scn: Scenario) {
    holder.rev
    if (s.finished) {
        Text(scn.title, style = MaterialTheme.typography.headlineSmall)
        Text("Path complete: ${scn.steps.last().name}.")
        return
    }
    val k = s.step
    val target = scn.step(k)
    val layer = scn.layer(k)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Layer ${Skeleton.layerOf(k) + 1}: ${layer.name} · step ${k + 1} of ${Skeleton.STEPS}", style = MaterialTheme.typography.labelLarge)
        WideSlot(slotName("obj", scn.id, k), SlotShape.SCENE, target.name)
        Text(target.name, style = MaterialTheme.typography.headlineSmall)
        Text("${target.kind} · ${Units.formatDistance(target.distance)} from ${scn.origin}", style = MaterialTheme.typography.bodyMedium)
        Text("Leg: ${Units.formatDistance(scn.legLength(k))} · drive: ${layer.drive} (${target.rung.label})", style = MaterialTheme.typography.bodySmall)
        if (target.fact.isNotBlank()) Text(target.fact, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        if (s.traveling) {
            LinearProgressIndicator(progress = { s.progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            val speed = scn.legLength(k) / e.legTime(s)
            Text("Arriving in ${Format.duration(e.travelTimeLeft(s))} · ${Units.formatSpeed(speed)}")
        } else {
            val cost = e.launchCost(k)
            Text("Fuel for this leg: ${Format.num(cost)} ${scn.main}")
            LinearProgressIndicator(progress = { if (cost <= 0) 1f else (s.main / cost).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { holder.act { en, st -> en.launch(st) } }, enabled = e.canLaunch(s), modifier = Modifier.fillMaxWidth()) {
                Text(if (e.canLaunch(s)) "Launch" else "Saving fuel…")
            }
            Text("Flight time at this drive: ${Format.duration(e.legTime(s))}", style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
        val cap = Skeleton.driveCap(s.layer)
        Text("Drive ${s.drive} / $cap · ×${"%.2f".format(Skeleton.driveMult(s.drive))} speed", style = MaterialTheme.typography.titleSmall)
        if (s.drive < cap) {
            Button(onClick = { holder.act { en, st -> en.buyDrive(st) } }, enabled = e.canBuyDrive(s), modifier = Modifier.fillMaxWidth()) {
                Text("Refit drive: ${Format.num(Skeleton.driveCost(s.drive))} ${scn.slow}")
            }
        } else Text("Maxed for this layer. The next layer raises the cap.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun IndustryRow(holder: GameHolder, e: Engine, s: GameState, scn: Scenario, t: Int) {
    holder.rev
    val unlocked = Skeleton.tierUnlocked(t, s.step)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtSlot(slotName("ind", scn.id, t), SlotShape.ICON, scn.layers[t].industry)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(scn.layers[t].industry, style = MaterialTheme.typography.titleSmall)
                if (unlocked) Text("Level ${s.industry[t]} · next ×2 at ${(s.industry[t] / Skeleton.MILESTONE_LEVELS + 1) * Skeleton.MILESTONE_LEVELS}", style = MaterialTheme.typography.bodySmall)
                else Text("Opens in layer ${t + 1}", style = MaterialTheme.typography.bodySmall)
            }
            if (unlocked) Button(onClick = { holder.act { en, st -> en.buyIndustry(st, t) } }, enabled = e.canBuyIndustry(s, t)) {
                Text(Format.num(Skeleton.industryCost(t, s.industry[t])))
            }
        }
    }
}

@Composable
private fun ResearchRow(holder: GameHolder, e: Engine, s: GameState, scn: Scenario, n: Int) {
    holder.rev
    val l = n / Skeleton.RESEARCH_PER_LAYER
    val name = scn.layers[l].research[n % Skeleton.RESEARCH_PER_LAYER]
    val open = Skeleton.researchUnlocked(n, s.step)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtSlot(slotName("res", scn.id, n), SlotShape.ICON, name)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(Format.researchEffect(n) + if (!open) " · layer ${l + 1}" else "", style = MaterialTheme.typography.bodySmall)
            }
            when {
                s.research[n] -> Text("Done")
                open -> Button(onClick = { holder.act { en, st -> en.buyResearch(st, n) } }, enabled = e.canBuyResearch(s, n)) {
                    Text(Format.num(Skeleton.researchCost(n)))
                }
            }
        }
    }
}

@Composable
private fun CrewRow(holder: GameHolder, s: GameState, scn: Scenario, u: Int) {
    holder.rev
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ArtSlot(slotName("crew", scn.id, u), SlotShape.ICON, scn.crew[u])
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(scn.crew[u], style = MaterialTheme.typography.titleSmall)
                Text(Format.crewJob(u), style = MaterialTheme.typography.bodySmall)
                Text(
                    if (s.crewHired(u)) "On duty" else "Joins at step ${Skeleton.CREW_STEP[u]} (${scn.step(Skeleton.CREW_STEP[u] - 1).name})",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun LogRow(scn: Scenario, k: Int) {
    val o = scn.step(k)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${k + 1}. ${o.name}", style = MaterialTheme.typography.titleSmall)
            Text("${o.kind} · ${Units.formatDistance(o.distance)} · ${o.rung.label}" + if (!o.catalogued) " · uncatalogued" else "", style = MaterialTheme.typography.labelSmall)
            Text(o.beat, style = MaterialTheme.typography.bodySmall)
            if (o.fact.isNotBlank()) Text(o.fact, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BoardTab(holder: GameHolder, e: Engine, s: GameState, scn: Scenario) {
    holder.rev
    var confirm by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Leaderboard", style = MaterialTheme.typography.titleMedium)
        Text("Your progress: ${"%.2f".format(e.progressScore(s))} of ${Skeleton.STEPS} · lifetime ${Format.num(s.lifetimeMain)} ${scn.main}")
        Text(
            "All worlds share one board because every world has the same steps and pace. The online board arrives with the server (docs/server-api.md).",
            style = MaterialTheme.typography.bodySmall,
        )
        HorizontalDivider()
        Text("Played ${Format.duration(s.time)} in ${scn.title}", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { confirm = true }) { Text("Choose another world") }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        confirmButton = { TextButton(onClick = { confirm = false; holder.abandon() }) { Text("Start over") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep playing") } },
        title = { Text("Leave this world?") },
        text = { Text("This phone keeps one game. Choosing another world deletes the progress in ${scn.title}.") },
    )
}
