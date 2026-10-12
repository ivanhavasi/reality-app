package cz.havasi.reality.app

import cz.havasi.reality.app.model.AdminJobStatus
import cz.havasi.reality.app.model.AdminJobType
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.service.AdminJobService
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// the real portal clients, pointed at a closed port: they fail fast and the providers return nothing
internal class UnreachablePortals : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> = mapOf(
        "quarkus.rest-client.\"sreality-api\".url" to "http://localhost:1",
        "quarkus.rest-client.\"bezrealitky-api\".url" to "http://localhost:1",
    )
}

@QuarkusTest
@TestProfile(UnreachablePortals::class)
internal class AdminJobBackfillIT {
    @Inject
    lateinit var adminJobService: AdminJobService

    // jobs run on threads Quarkus didn't create; the portal REST clients are built there on first use
    @Test
    fun `backfill builds the real portal clients on its background threads`() {
        val started = adminJobService.start(AdminJobType.BACKFILL_COORDINATES, BuildingType.APARTMENT, TransactionType.SALE)

        val deadline = System.currentTimeMillis() + 30_000
        var job = started
        while ((job.status == AdminJobStatus.WAITING || job.status == AdminJobStatus.RUNNING) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
            job = adminJobService.recentJobs().first { it.id == started.id }
        }

        assertEquals(null, job.error)
        assertEquals(AdminJobStatus.SUCCEEDED, job.status)
    }
}
