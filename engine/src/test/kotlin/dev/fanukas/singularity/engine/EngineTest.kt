package dev.fanukas.singularity.engine

import kotlin.test.Test

class EngineTest {
    @Test fun pacing() = println(EngineChecks.pacingMatchesReferenceCurve())
    @Test fun table() = println(EngineChecks.tableIsSolvedFromSkeleton())
    @Test fun offlineTicks() = println(EngineChecks.offlineTicksKeepPace())
    @Test fun scenarios() = println(EngineChecks.everyScenarioIsValid())
    @Test fun save() = println(EngineChecks.saveRoundTrips())
    @Test fun offlineCap() = println(EngineChecks.offlineIsCapped())
    @Test fun crew() = println(EngineChecks.noCrewNoAutoBuying())
    @Test fun hooks() = println(EngineChecks.sideHooksNeverChangeTheMainPath())
}
