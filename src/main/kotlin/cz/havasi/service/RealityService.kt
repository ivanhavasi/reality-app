package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.ApartmentDuplicate
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
import kotlin.math.abs
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

    public suspend fun fetchAndSaveApartmentsForSale(): Unit {
        Log.info("Fetching and saving apartments for sale")
        estateProviders.forEach {
            launchAndHandleException {
                fetchAndSaveApartmentsForProvider(it)
            }
        }
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

        Log.debug("Found ${apartments.size} apartments for provider $this (offset=$offset, limit=$limit)")

        val filteredApartments = apartments
            .mapNotNull {
                Log.debug("Processing apartment ${it.name} (id=${it.id})")
                val duplicateApartment = apartmentRepository.findByIdOrFingerprint(it.id, it.fingerprint)

                // size of the apartment is the same
                if (duplicateApartment != null
                    && areDoublesEqualWithTolerance(duplicateApartment.sizeInM2, it.sizeInM2)
                ) {
                    it.updateWithDuplicateIfNeeded(duplicateApartment)
                } else {
                    it
                }
            }

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

    private fun Apartment.updateWithDuplicateIfNeeded(duplicateApartment: Apartment): Apartment? {
        val foundProviders = hashSetOf(provider) + duplicates.map { it.provider }
        if (price == duplicateApartment.price && foundProviders.contains(duplicateApartment.provider)) {
            return null // ignore, if price is the same
        }
        val duplicateList = duplicates.toMutableList()
        duplicateList.add(duplicateApartment.toDuplicate())
        return this.copy(duplicates = duplicateList)
    }

    private suspend fun launchAndHandleException(f: suspend () -> Unit) = coroutineScope {
        launch {
            try {
                f()
            } catch (e: Exception) {
                Log.error("Error while fetching and saving apartments", e)
                throw e
            }
        }
    }

    private fun Apartment.toDuplicate() =
        ApartmentDuplicate(
            url = url,
            price = price,
            pricePerM2 = pricePerM2,
            images = images,
            provider = provider,
        )

    private fun areDoublesEqualWithTolerance(a: Double, b: Double, tolerance: Double = 0.05): Boolean {
        val difference = abs(a - b)

        return difference <= abs(a).coerceAtLeast(abs(b)) * tolerance
    }
}
