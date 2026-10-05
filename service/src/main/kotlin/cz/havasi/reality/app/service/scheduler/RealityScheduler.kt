package cz.havasi.reality.app.service.scheduler

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.service.RealEstateService
import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ApplicationScoped
internal class RealityScheduler(
    private val realEstateService: RealEstateService,
) {
    // the jobs hit the same portals, so they never run at the same time
    private val fetchMutex = Mutex()

    @Scheduled(cron = "{reality.scheduler.cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    internal suspend fun scheduleRealityRetrieval() {
        Log.info("Scheduled task started")

        waitForRandomInterval()
        fetchMutex.withLock {
            fetch(BuildingType.APARTMENT, TransactionType.SALE)
            waitForRandomInterval(120)
            fetch(BuildingType.APARTMENT, TransactionType.RENT)
        }
        Log.info("Scheduled task finished")
    }

    // phase 1 scrapes building plots only; if subtypes become configurable, inject MutableList<LandSubCategory>,
    // a Kotlin List<Enum> config property breaks creation of this bean and stops the apartment job too
    @Scheduled(cron = "{reality.scheduler.land-cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    internal suspend fun scheduleLandRetrieval() {
        Log.info("Scheduled land task started")

        waitForRandomInterval(300)
        fetchMutex.withLock {
            fetch(BuildingType.LAND, TransactionType.SALE, LandSubCategory.BUILDING_PLOT)
        }
        Log.info("Scheduled land task finished")
    }

    private suspend fun fetch(
        buildingType: BuildingType,
        transactionType: TransactionType,
        landSubCategory: LandSubCategory? = null,
    ) {
        Log.info("Fetching and saving $buildingType $transactionType ${landSubCategory ?: ""}")
        try {
            realEstateService.fetchAndSaveRealEstate(buildingType, transactionType, landSubCategory)
        } catch (e: Exception) {
            Log.error("Fetching $buildingType $transactionType failed", e)
        }
    }

    private suspend fun waitForRandomInterval(maxInterval: Long = 600) {
        val randomInterval = (1..maxInterval).random() // wait up to 600 seconds
        Log.info("Waiting for $randomInterval seconds")
        delay(randomInterval * 1000)
    }
}
