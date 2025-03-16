package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.BuildingType
import cz.havasi.model.TransactionType
import cz.havasi.model.command.GetEstatesCommand
import cz.havasi.repository.ApartmentRepository
import cz.havasi.service.provider.EstatesProvider
import io.quarkus.arc.All
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@ApplicationScoped
public class RealityService(
    private val apartmentRepository: ApartmentRepository,
    private val notificationService: NotificationService,
    @All private val estateProviders: MutableList<EstatesProvider>,
) {
    init {
        Log.debug("Estate providers: $estateProviders")
    }

    public suspend fun fetchAndSaveApartmentsForSale(): Unit = coroutineScope {
        Log.info("Fetching and saving apartments for sale")
        estateProviders.forEach { launch { fetchAndSaveApartmentsForProvider(it) } }
    }

    private suspend fun fetchAndSaveApartmentsForProvider(provider: EstatesProvider) {
        val numberOfApartments = 22
        val maxCalls = 5

        for (i in 0 until maxCalls) {
            Log.info("Fetching apartments for provider $provider, call $i")
            val apartments = provider
                .getApartments(i * numberOfApartments, numberOfApartments)
                .saveApartments()
                .sendNotifications()

            if (apartments.isEmpty()) {
                Log.info("No more apartments to save for provider $provider")
                break
            }
            delay(Random.nextLong(700, 2500))
        }
    }

    private suspend fun EstatesProvider.getApartments(offset: Int, limit: Int): List<Apartment> {
        val apartments = getEstates(
            GetEstatesCommand(
                type = BuildingType.APARTMENT,
                transaction = TransactionType.SALE,
                offset = offset,
                limit = limit,
            ),
        )

        val filteredApartments = apartments
            .filter {
                !apartmentRepository.existsByIdOrFingerprint(
                    it.id,
                    it.fingerprint,
                )
            } // todo, just update date in already existant apartments?

        Log.info("Saving ${filteredApartments.size} apartments (vs ${apartments.size} fetched)")
        return filteredApartments
    }

    private suspend fun List<Apartment>.sendNotifications() = also {
        if (isNotEmpty()) {
            notificationService.sendNotificationsForApartments(this)
        }
    }

    private suspend fun List<Apartment>.saveApartments(): List<Apartment> = also {
        if (isNotEmpty()) {
            apartmentRepository.saveAll(this)
        }
    }
}
