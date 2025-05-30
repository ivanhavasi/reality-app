package cz.havasi.reality.app.service.scheduler

import cz.havasi.reality.app.service.RealEstateService
import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
internal class RealityScheduler(
    private val realEstateService: RealEstateService,
    @ConfigProperty(name = "reality.scheduler.cron") private val cron: String, // todo cron
) {
    @Scheduled(cron = "0 */30 6-23 * * ?")
    internal suspend fun scheduleRealityRetrieval() {
        Log.info("Scheduled task started")

        waitForRandomInterval()
        try {
            realEstateService.fetchAndSaveApartmentsForSale()
        } catch (e: Exception) {
            Log.error(e.message)
            Log.error(e.stackTraceToString())
            throw e
        }

        Log.info("Scheduled task finished")
    }

    private suspend fun waitForRandomInterval() {
        val randomInterval = (1..600).random().toLong() // wait up to 10 minutes
        Log.info("Waiting for $randomInterval seconds")
        delay(randomInterval * 1000)
    }
}
