package cz.havasi.scheduler

import cz.havasi.service.RealityService
import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
internal class RealityScheduler(
    private val realityService: RealityService,
    @ConfigProperty(name = "reality.scheduler.cron") private val cron: String,
) {
    @Scheduled(cron = "0 */30 6-23 * * ?")
    internal suspend fun scheduleRealityRetrieval() {
        Log.info("Scheduled task started")

        waitForRandomInterval()
        realityService.fetchAndSaveApartmentsForSale()

        Log.info("Scheduled task finished")
    }

    private suspend fun waitForRandomInterval() {
        val randomInterval = (1..600).random().toLong() // wait up to 10 minutes
        Log.info("Waiting for $randomInterval seconds")
        delay(randomInterval * 1000)
    }
}
