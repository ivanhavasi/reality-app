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

        Log.info("Found ${apartments.size} apartments for provider $this (offset=$offset, limit=$limit)")

        val filteredApartments = apartments
            .mapNotNull {
                Log.info("Processing apartment ${it.name} (id=${it.id})")
                val duplicateApartment = apartmentRepository.findByIdOrFingerprint(it.id, it.fingerprint)
                Log.info("Found duplicate apartment or null ${duplicateApartment?.id} for ${it.name}")
                if (duplicateApartment != null) {
                    Log.info("Duplicate exists for ${it.name} (id=${duplicateApartment.id})")
                    it.updateWithDuplicateIfNeeded(duplicateApartment)
                } else {
                    Log.info("Saving new apartment ${it.name}")
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
        val foundProviders = mutableListOf(provider) + duplicates.map { it.provider }
        if (price == duplicateApartment.price && foundProviders.contains(duplicateApartment.provider)) {
            null // ignore, if price is the same
        }
        val duplicateList = duplicates.toMutableList()
        duplicateList.add(duplicateApartment.toDuplicate())
        return this.copy(duplicates = duplicateList)
    }

    private fun Apartment.toDuplicate() =
        ApartmentDuplicate(
            url = url,
            price = price,
            pricePerM2 = pricePerM2,
            images = images,
            provider = provider,
        )
}
