package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.ApartmentDuplicate
import cz.havasi.model.BuildingType
import cz.havasi.model.TransactionType
import cz.havasi.model.command.ApartmentsAndDuplicates
import cz.havasi.model.command.GetRealEstatesCommand
import cz.havasi.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.model.util.Paging
import cz.havasi.repository.ApartmentRepository
import cz.havasi.service.provider.RealEstatesProvider
import cz.havasi.service.util.areDoublesEqualWithTolerance
import cz.havasi.service.util.forEachAsync
import io.quarkus.arc.All
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import kotlin.random.Random

@ApplicationScoped
public class RealEstateService(
    private val apartmentRepository: ApartmentRepository,
    private val notificationService: NotificationService,
    @All private val realEstateProviders: MutableList<RealEstatesProvider>,
) {
    init {
        Log.debug("Real Estate providers: $realEstateProviders")
    }

    public suspend fun fetchAndSaveApartmentsForSale(): Unit {
        realEstateProviders.forEachAsync(message = "Fetching and saving apartments") {
            fetchAndSaveApartmentsForProvider(it)
        }
    }

    public suspend fun getApartments(paging: Paging): List<Apartment> =
        apartmentRepository
            .findAll(paging)

    private suspend fun fetchAndSaveApartmentsForProvider(provider: RealEstatesProvider) {
        val numberOfApartments = 22
        val maxCalls = 5

        for (i in 0 until maxCalls) {
            val apartments = provider
                .getApartments(i * numberOfApartments, numberOfApartments)
                .filterApartments()
                .saveApartments()
                .sendNotifications()

            if (apartments.isEmpty()) {
                Log.debug("No more apartments to save for provider $provider")
                break
            }
            delay(Random.nextLong(700, 2500))
        }
    }

    private suspend fun RealEstatesProvider.getApartments(offset: Int, limit: Int): List<Apartment> =
        getRealEstates(
            GetRealEstatesCommand(
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

        return ApartmentsAndDuplicates(newApartments, duplicates)
    }

    private suspend fun findOriginalApartment(apartment: Apartment): Apartment? {
        val apartments = apartmentRepository.findByIdOrFingerprint(apartment.id, apartment.fingerprint)
        val exactApartment = apartments.firstOrNull { it.id == apartment.id }

        return exactApartment
            ?: apartments.firstOrNull { areDoublesEqualWithTolerance(apartment.sizeInM2, it.sizeInM2) }
    }

    private fun areApartmentsDuplicates(apartment: Apartment, originalApartment: Apartment?): Boolean =
        originalApartment != null
            && (
            apartment.id == originalApartment.id
                || areDoublesEqualWithTolerance(originalApartment.sizeInM2, apartment.sizeInM2)
            )

    private fun shouldApartmentBeSavedAsDuplicate(duplicate: Apartment, originalApartment: Apartment): Boolean {
        val foundProviders = hashSetOf(originalApartment.provider) + originalApartment.duplicates.map { it.provider }
        var minPrice = originalApartment.duplicates.minOfOrNull { it.price } ?: originalApartment.price
        minPrice = minOf(minPrice, originalApartment.price)

        return (duplicate.price < minPrice || !foundProviders.contains(duplicate.provider))
    }

    private suspend fun List<Apartment>.sendNotifications() = also {
        notificationService.sendNotificationsForApartments(this)
    }

    private suspend fun ApartmentsAndDuplicates.saveApartments(): List<Apartment> = let {
        if (apartments.isNotEmpty()) {
            apartmentRepository.saveAll(apartments)
        }
        if (duplicates.isNotEmpty()) {
            apartmentRepository.bulkUpdateApartmentWithDuplicate(duplicates)
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
