package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.AdminJob
import cz.havasi.reality.app.model.AdminJobStatus
import cz.havasi.reality.app.model.AdminJobType
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.ProviderType.SREALITY
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// real dispatchers: the jobs run on their own scope, so these wait for them instead of using virtual time
internal class AdminJobServiceTest {
    private val portalLock = PortalLock()
    private val fixture = RealEstateServiceFixture(
        listOf(
            FakeProvider("portal", callDelayMillis = 200) { page ->
                if (page == 0) listOf(apartment("s1", SREALITY, 54.0, 8_000_000.0)) else emptyList()
            },
        ),
    )
    private val jobs = AdminJobService(fixture.service, portalLock)

    @AfterEach
    fun stop() = jobs.shutdown()

    private fun awaitFinished(id: String): AdminJob = runBlocking {
        withTimeout(15_000) {
            var job = jobs.recentJobs().first { it.id == id }
            while (job.status == AdminJobStatus.WAITING || job.status == AdminJobStatus.RUNNING) {
                delay(50)
                job = jobs.recentJobs().first { it.id == id }
            }
            job
        }
    }

    @Test
    fun `scrape runs in the background and reports how many listings it saved`() {
        val started = jobs.start(AdminJobType.SCRAPE, BuildingType.APARTMENT, TransactionType.SALE)

        val finished = awaitFinished(started.id)

        assertEquals(AdminJobStatus.SUCCEEDED, finished.status)
        assertEquals(1, finished.savedListings)
        assertEquals(listOf("s1"), fixture.repository.stored.map { it.id })
    }

    @Test
    fun `only one job runs at a time`() {
        val started = jobs.start(AdminJobType.SCRAPE, BuildingType.APARTMENT, TransactionType.SALE)

        assertFailsWith<AdminJobAlreadyRunningException> {
            jobs.start(AdminJobType.BACKFILL_COORDINATES, BuildingType.APARTMENT, TransactionType.SALE)
        }
        awaitFinished(started.id)
    }

    @Test
    fun `job waits while a scheduled scrape holds the portal lock`() = runBlocking {
        val started = portalLock.mutex.withLock {
            val job = jobs.start(AdminJobType.SCRAPE, BuildingType.APARTMENT, TransactionType.SALE)
            delay(300)
            assertEquals(AdminJobStatus.WAITING, jobs.recentJobs().first { it.id == job.id }.status)
            job
        }

        assertEquals(AdminJobStatus.SUCCEEDED, awaitFinished(started.id).status)
    }
}
