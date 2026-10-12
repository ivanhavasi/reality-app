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
    private val placeImportService: PlaceImportService,
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + ContextClassLoader(AdminJobService::class.java.classLoader),
    )
    private val jobs = mutableListOf<AdminJob>()

    public fun recentJobs(): List<AdminJob> = synchronized(jobs) { jobs.reversed() }

    public fun start(type: AdminJobType, buildingType: BuildingType? = null, transactionType: TransactionType? = null): AdminJob {
        require(type == AdminJobType.IMPORT_PLACES || (buildingType != null && transactionType != null)) {
            "$type needs a building and a transaction type"
        }
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
        try {
            if (job.type == AdminJobType.IMPORT_PLACES) {
                // OpenStreetMap, not the portals: no need to wait for a scheduled scrape
                update(job.id) { it.copy(status = AdminJobStatus.RUNNING, startedAt = now()) }
                placeImportService.importAll { progress ->
                    update(job.id) {
                        it.copy(placeCategoriesImported = progress.categoriesImported, placesImported = progress.placesImported)
                    }
                }
            } else {
                portalLock.mutex.withLock { runOnPortals(job, job.buildingType!!, job.transactionType!!) }
            }
            update(job.id) { it.copy(status = AdminJobStatus.SUCCEEDED, finishedAt = now()) }
        } catch (e: Exception) {
            Log.error("Admin job ${job.type} ${job.buildingType ?: ""} ${job.transactionType ?: ""} failed", e)
            update(job.id) { it.copy(status = AdminJobStatus.FAILED, finishedAt = now(), error = e.message ?: e.javaClass.simpleName) }
        }
    }

    private suspend fun runOnPortals(job: AdminJob, buildingType: BuildingType, transactionType: TransactionType) {
        val landSubCategory = LandSubCategory.BUILDING_PLOT.takeIf { buildingType == BuildingType.LAND }
        update(job.id) { it.copy(status = AdminJobStatus.RUNNING, startedAt = now()) }
        when (job.type) {
            AdminJobType.SCRAPE -> {
                val saved = realEstateService.fetchAndSaveRealEstate(buildingType, transactionType, landSubCategory)
                update(job.id) { it.copy(savedListings = saved) }
            }

            AdminJobType.BACKFILL_COORDINATES ->
                realEstateService.backfillCoordinates(buildingType, transactionType, landSubCategory) { progress ->
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

            AdminJobType.IMPORT_PLACES -> error("The places import doesn't use the portals")
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
