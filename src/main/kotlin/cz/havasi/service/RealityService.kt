package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.ApartmentDuplicate
import cz.havasi.model.BuildingType
import cz.havasi.model.TransactionType
import cz.havasi.model.command.ApartmentsAndDuplicates
import cz.havasi.model.command.GetEstatesCommand
import cz.havasi.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.repository.ApartmentRepository
import cz.havasi.service.provider.EstatesProvider
import cz.havasi.service.util.areDoublesEqualWithTolerance
import cz.havasi.service.util.forEachAsync
//import cz.havasi.service.util.launchAndHandleException
import io.quarkus.arc.All
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
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
        estateProviders.forEachAsync(message = "Fetching and saving apartments") {
            fetchAndSaveApartmentsForProvider(it)
        }
    }

    private suspend fun fetchAndSaveApartmentsForProvider(provider: EstatesProvider) {
        val numberOfApartments = 22
        val maxCalls = 5

        for (i in 0 until maxCalls) {
            Log.info("Fetching apartments for provider $provider, call $i")
            val apartments = provider
                .getApartments(i * numberOfApartments, numberOfApartments)
                .filterApartments()
                .saveApartments()
                .sendNotifications()

            if (apartments.isEmpty()) {
                Log.info("No more apartments to save for provider $provider")
                break
            }
            delay(Random.nextLong(700, 2500))
        }
    }

    private suspend fun EstatesProvider.getApartments(offset: Int, limit: Int): List<Apartment> =
        getEstates(
            GetEstatesCommand(
                type = BuildingType.APARTMENT,
                transaction = TransactionType.SALE,
                offset = offset,
                limit = limit,
            ),
        )
            .also { Log.debug("Found ${it.size} apartments for provider $this (offset=$offset, limit=$limit)") }

    private suspend fun List<Apartment>.filterApartments(): ApartmentsAndDuplicates {
        val duplicates = mutableListOf<UpdateApartmentWithDuplicateCommand>()
        val newApartments = mutableListOf<Apartment>()

        forEach {
            val originalApartment = findOriginalApartment(it)
            if (areApartmentsDuplicates(it, originalApartment)) {
                if (shouldApartmentBeSavedAsDuplicate(it, originalApartment!!)) { // null-check- areApartmentsDuplicates
                    duplicates.add(UpdateApartmentWithDuplicateCommand(originalApartment, it.toDuplicate()))
                }
            } else {
                newApartments.add(it)
            }
        }

        Log.info("Saving ${newApartments.size} apartments and ${duplicates.size} duplicates (vs $size fetched)")
        return ApartmentsAndDuplicates(newApartments, duplicates)
    }

    private suspend fun findOriginalApartment(apartment: Apartment): Apartment? {
        val apartments = apartmentRepository.findByIdOrFingerprint(apartment.id, apartment.fingerprint)
        val exactApartment = apartments.firstOrNull { it.id == apartment.id }

        return exactApartment
            ?: apartments.firstOrNull { areDoublesEqualWithTolerance(apartment.sizeInM2, it.sizeInM2) }
    }

    private fun areApartmentsDuplicates(apartment: Apartment, originalApartment: Apartment?): Boolean =
        originalApartment != null && areDoublesEqualWithTolerance(originalApartment.sizeInM2, apartment.sizeInM2)

    private fun shouldApartmentBeSavedAsDuplicate(duplicate: Apartment, originalApartment: Apartment): Boolean {
        val foundProviders = hashSetOf(originalApartment.provider) + originalApartment.duplicates.map { it.provider }
        var minPrice = originalApartment.duplicates.minOfOrNull { it.price } ?: originalApartment.price
        minPrice = minOf(minPrice, originalApartment.price)

        Log.info("Duplicate resolution for apartmentId ${originalApartment.id} (duplicateId ${duplicate.id}), found providers: $foundProviders, its provider ${originalApartment.provider}, price ${originalApartment.price} (min price $minPrice), duplicate price ${duplicate.price}")
        if (duplicate.price >= minPrice && foundProviders.contains(duplicate.provider)) {
            Log.info("Ignoring duplicate apartment ${duplicate.id}, because its price is the same or higher than the original apartment")
            return false
        }
        Log.info("Continueing with duplicate apartment ${duplicate.id}, because its price is lower than the original apartment")
        return true
    }

    private suspend fun List<Apartment>.sendNotifications() = also {
        notificationService.sendNotificationsForApartments(this)
    }

    private suspend fun ApartmentsAndDuplicates.saveApartments(): List<Apartment> = let {
        if (apartments.isNotEmpty()) {
            try {
                apartmentRepository.saveAll(apartments)
            } catch (e: Exception) {
                Log.error("2 chybaaaa")
                Log.error(e.message)
            }
        }
        if (duplicates.isNotEmpty()) {
            try {
                apartmentRepository.bulkUpdateApartmentWithDuplicate(duplicates)
            } catch (e: Exception) {
                Log.error("ASDASDASD")
                Log.error(e)
            }
        }

        apartments + duplicates.map { it.apartment.addDuplicate(it.duplicate) }
    }

    private fun Apartment.addDuplicate(duplicate: ApartmentDuplicate) =
        copy(duplicates = (duplicates + duplicate))

    private fun Apartment.toDuplicate() =
        ApartmentDuplicate(
            url = url,
            price = price,
            pricePerM2 = pricePerM2,
            images = images,
            provider = provider,
        )
}
