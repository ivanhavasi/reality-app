package cz.havasi.reality.app.service.scheduler

import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.service.RealEstateService
import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
internal class RealityScheduler(
    private val realEstateService: RealEstateService,
    @ConfigProperty(name = "reality.scheduler.cron") private val cron: String,
) {
    @Scheduled(cron = "0 */20 6-23 * * ?")
    internal suspend fun scheduleRealityRetrieval() {
        Log.info("Scheduled task started")

        waitForRandomInterval()
        try {
            realEstateService.fetchAndSaveRealEstate(BuildingType.APARTMENT, TransactionType.SALE)
        } catch (e: Exception) {
            Log.error(e.message)
            Log.error(e.stackTraceToString())
        }
        waitForRandomInterval(120)
        Log.info("Fetching and saving apartments for rent")
        try {
            realEstateService.fetchAndSaveRealEstate(BuildingType.APARTMENT, TransactionType.RENT)
        } catch (e: Exception) {
            Log.error(e.message)
            Log.error(e.stackTraceToString())
        }
        Log.info("Scheduled task finished")
    }

    private suspend fun waitForRandomInterval(maxInterval: Long = 600) {
        val randomInterval = (1..maxInterval).random() // wait up to 600 seconds
        Log.info("Waiting for $randomInterval seconds")
        delay(randomInterval * 1000)
    }
}
