package cz.havasi.reality.app.service.scheduler

import cz.havasi.reality.app.service.PlaceImportService
import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
internal class PlaceScheduler(
    private val placeImportService: PlaceImportService,
) {
    @Scheduled(cron = "{reality.scheduler.places-cron}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    internal suspend fun schedulePlacesImport() {
        Log.info("Scheduled places import started")
        try {
            val progress = placeImportService.importAll()
            Log.info("Scheduled places import finished: ${progress.placesImported} places")
        } catch (e: Exception) {
            Log.error("Scheduled places import failed", e)
        }
    }
}
