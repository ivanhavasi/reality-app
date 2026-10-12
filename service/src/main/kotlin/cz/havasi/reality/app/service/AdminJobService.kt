package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.AdminJob
import cz.havasi.reality.app.model.AdminJobStatus
import cz.havasi.reality.app.model.AdminJobType
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.service.util.ContextClassLoader
import io.quarkus.logging.Log
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

public class AdminJobAlreadyRunningException : IllegalStateException("Another admin job is still running")

// jobs outlive the request that starts them: a backfill takes minutes, longer than the proxy keeps a request open.
// They are kept in memory only, so a restart forgets them
@ApplicationScoped
public class AdminJobService(
    private val realEstateService: RealEstateService,
    private val portalLock: PortalLock,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + ContextClassLoader(AdminJobService::class.java.classLoader),
    )
    private val jobs = mutableListOf<AdminJob>()

    public fun recentJobs(): List<AdminJob> = synchronized(jobs) { jobs.reversed() }

    public fun start(type: AdminJobType, buildingType: BuildingType, transactionType: TransactionType): AdminJob {
        val job = synchronized(jobs) {
            if (jobs.any { it.status == AdminJobStatus.WAITING || it.status == AdminJobStatus.RUNNING }) {
                throw AdminJobAlreadyRunningException()
            }
            AdminJob(
                id = UUID.randomUUID().toString(),
                type = type,
                buildingType = buildingType,
                transactionType = transactionType,
                status = AdminJobStatus.WAITING,
                createdAt = now(),
            ).also {
                jobs += it
                if (jobs.size > MAX_KEPT_JOBS) jobs.removeAt(0)
            }
        }
        scope.launch { run(job) }
        return job
    }

    @PreDestroy
    internal fun shutdown() {
        scope.cancel()
    }

    private suspend fun run(job: AdminJob) {
        val landSubCategory = LandSubCategory.BUILDING_PLOT.takeIf { job.buildingType == BuildingType.LAND }
        try {
            portalLock.mutex.withLock {
                update(job.id) { it.copy(status = AdminJobStatus.RUNNING, startedAt = now()) }
                when (job.type) {
                    AdminJobType.SCRAPE -> {
                        val saved = realEstateService.fetchAndSaveRealEstate(job.buildingType, job.transactionType, landSubCategory)
                        update(job.id) { it.copy(savedListings = saved) }
                    }

                    AdminJobType.BACKFILL_COORDINATES ->
                        realEstateService.backfillCoordinates(job.buildingType, job.transactionType, landSubCategory) { progress ->
                            update(job.id) {
                                it.copy(
                                    pagesScanned = progress.pagesScanned,
                                    listingsSeen = progress.listingsSeen,
                                    coordinatesFilled = progress.coordinatesFilled,
                                    addressesToLocate = progress.addressesToLocate,
                                    addressesChecked = progress.addressesChecked,
                                    addressesLocated = progress.addressesLocated,
                                )
                            }
                        }
                }
            }
            update(job.id) { it.copy(status = AdminJobStatus.SUCCEEDED, finishedAt = now()) }
        } catch (e: Exception) {
            Log.error("Admin job ${job.type} ${job.buildingType} ${job.transactionType} failed", e)
            update(job.id) { it.copy(status = AdminJobStatus.FAILED, finishedAt = now(), error = e.message ?: e.javaClass.simpleName) }
        }
    }

    private fun update(id: String, change: (AdminJob) -> AdminJob) {
        synchronized(jobs) {
            val index = jobs.indexOfFirst { it.id == id }
            if (index >= 0) jobs[index] = change(jobs[index])
        }
    }

    private fun now() = OffsetDateTime.now(UTC)

    private companion object {
        const val MAX_KEPT_JOBS = 20
    }
}
