package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.ApartmentDuplicate
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.ApartmentsAndDuplicates
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.service.provider.RealEstatesProvider
import cz.havasi.reality.app.service.repository.ApartmentRepository
import cz.havasi.reality.app.service.util.areDoublesEqualWithTolerance
import cz.havasi.reality.app.service.util.forEachAsync
import cz.havasi.reality.app.service.util.forEachSequentially
import cz.havasi.reality.app.service.util.toComparableCityPart
import io.quarkus.arc.All
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import kotlin.random.Random

@ApplicationScoped
public class RealEstateService(
    private val apartmentRepository: ApartmentRepository,
    private val userNotificationService: UserNotificationService,
    @All private val realEstateProviders: MutableList<RealEstatesProvider>,
) {
    public suspend fun fetchAndSaveRealEstate(
        buildingType: BuildingType,
        transactionType: TransactionType,
        landSubCategory: LandSubCategory? = null,
    ) {
        // land runs providers one after another, so a plot listed on two portals in the same run is merged, not stored twice
        if (buildingType == BuildingType.LAND) {
            realEstateProviders.forEachSequentially(message = "Fetching and saving land") {
                fetchAndSaveApartmentsForProvider(it, buildingType, transactionType, landSubCategory)
            }
        } else {
            realEstateProviders.forEachAsync(message = "Fetching and saving apartments") {
                fetchAndSaveApartmentsForProvider(it, buildingType, transactionType, landSubCategory)
            }
        }
    }

    public suspend fun findRealEstates(command: FindRealEstatesCommand): List<Apartment> =
        apartmentRepository.findAll(command)

    public suspend fun getById(id: String): Apartment =
        apartmentRepository.findByIdOrFingerprint(id, "").let { found -> found.firstOrNull { it.id == id } ?: found.firstOrNull() }
            ?: throw NoSuchElementException("Apartment with id $id not found")

    private suspend fun fetchAndSaveApartmentsForProvider(
        provider: RealEstatesProvider,
        buildingType: BuildingType,
        transactionType: TransactionType,
        landSubCategory: LandSubCategory?,
    ) {
        val numberOfApartments = 22
        val maxCalls = if (buildingType == BuildingType.LAND) 12 else 5

        for (i in 0 until maxCalls) {
            val fetched = provider
                .getApartments(i * numberOfApartments, numberOfApartments, buildingType, transactionType, landSubCategory)
            if (fetched.isEmpty()) {
                Log.debug("No more apartments to fetch for provider $provider")
                break
            }
            val saved = fetched
                .filterApartments()
                .saveApartments()
                .sendNotifications()

            // portals reorder land by "bump", so new plots can sit past the first page: land always scans every page
            if (saved.isEmpty() && buildingType != BuildingType.LAND) {
                Log.debug("No more apartments to save for provider $provider")
                break
            }
            delay(Random.nextLong(700, 2500))
        }
    }

    private suspend fun RealEstatesProvider.getApartments(
        offset: Int,
        limit: Int,
        buildingType: BuildingType,
        transactionType: TransactionType,
        landSubCategory: LandSubCategory?,
    ): List<Apartment> =
        getRealEstates(
            GetRealEstatesCommand(
                type = buildingType,
                transaction = transactionType,
                offset = offset,
                limit = limit,
                landSubCategory = landSubCategory,
            ),
        )
            .also { Log.debug("Found ${it.size} apartments for provider $this (offset=$offset, limit=$limit)") }

    private suspend fun List<Apartment>.filterApartments(): ApartmentsAndDuplicates {
        val duplicates = mutableListOf<UpdateApartmentWithDuplicateCommand>()
        val newApartments = mutableListOf<Apartment>()
        // duplicates are saved after the whole page, so land tracks them here: otherwise two listings of one portal
        // on the same page could both attach to the same plot
        val pendingLandOriginals = mutableMapOf<String, Apartment>()

        forEach {
            val originalApartment = findOriginalApartment(it, pendingLandOriginals)
            if (areApartmentsDuplicates(it, originalApartment)) {
                if (shouldApartmentBeSavedAsDuplicate(it, originalApartment!!)) { // null-check- areApartmentsDuplicates
                    val originalApartmentWithLocation = resolveLocationFromDuplicate(originalApartment, it)
                    duplicates.add(UpdateApartmentWithDuplicateCommand(originalApartmentWithLocation, it.toDuplicate()))
                    if (it.mainCategory == BuildingType.LAND) {
                        pendingLandOriginals[originalApartment.id] = originalApartment.addDuplicate(it.toDuplicate())
                    }
                }
            } else {
                newApartments.add(it)
            }
        }

        return ApartmentsAndDuplicates(newApartments, duplicates)
    }

    private fun resolveLocationFromDuplicate(
        originalApartment: Apartment,
        duplicate: Apartment,
    ): Apartment =
        if (originalApartment.locality.latitude == null
            && originalApartment.locality.longitude == null
            && duplicate.locality.latitude != null
            && duplicate.locality.longitude != null
        ) {
            originalApartment.copy(
                locality = originalApartment.locality.copy(
                    latitude = duplicate.locality.latitude,
                    longitude = duplicate.locality.longitude,
                ),
            )
        } else {
            originalApartment
        }

    private suspend fun findOriginalApartment(
        apartment: Apartment,
        pendingLandOriginals: Map<String, Apartment>,
    ): Apartment? {
        val candidates = apartmentRepository.findByIdOrFingerprint(apartment.id, apartment.fingerprint)
            .map { pendingLandOriginals[it.id] ?: it }

        return candidates.firstOrNull { it.id == apartment.id } ?: when (apartment.mainCategory) {
            // a known duplicate wins over a newer plot with the same area
            BuildingType.LAND -> candidates.firstOrNull { apartment.id in it.duplicateIds() }
                ?: candidates.firstOrNull { it.mainCategory == BuildingType.LAND && it.isSameRealEstateAs(apartment) }

            else -> candidates.firstOrNull { it.fingerprint == apartment.fingerprint && it.isSameRealEstateAs(apartment) }
        }
    }

    private fun areApartmentsDuplicates(apartment: Apartment, originalApartment: Apartment?): Boolean =
        originalApartment != null
            && originalApartment.transactionType == apartment.transactionType
            && originalApartment.mainCategory == apartment.mainCategory
            && (apartment.id == originalApartment.id || originalApartment.isSameRealEstateAs(apartment))

    private fun Apartment.isSameRealEstateAs(other: Apartment): Boolean =
        when (mainCategory) {
            // within one provider a different id is never the same plot; Prague is one "city", so cross-portal merges need the guard
            BuildingType.LAND -> other.id in duplicateIds()
                || (other.provider != provider
                && duplicates.none { it.provider == other.provider }
                && hasSimilarPrice(other)
                && !hasConflictingCityPart(other))

            else -> areDoublesEqualWithTolerance(sizeInM2, other.sizeInM2)
        }

    private fun Apartment.duplicateIds(): List<String> = duplicates.mapNotNull { it.id }

    private fun Apartment.hasSimilarPrice(other: Apartment): Boolean =
        price <= 0 || other.price <= 0 || areDoublesEqualWithTolerance(price, other.price, LAND_PRICE_TOLERANCE)

    private fun Apartment.hasConflictingCityPart(other: Apartment): Boolean {
        val mine = locality.district.toComparableCityPart()
        val theirs = other.locality.district.toComparableCityPart()
        return mine != null && theirs != null && mine != theirs
    }

    private fun shouldApartmentBeSavedAsDuplicate(duplicate: Apartment, originalApartment: Apartment): Boolean {
        val foundProviders = hashSetOf(originalApartment.provider) + originalApartment.duplicates.map { it.provider }
        var minPrice = originalApartment.duplicates.minOfOrNull { it.price } ?: originalApartment.price
        minPrice = minOf(minPrice, originalApartment.price)

        return (duplicate.price < minPrice || !foundProviders.contains(duplicate.provider))
    }

    private suspend fun List<Apartment>.sendNotifications() = also {
        userNotificationService.sendUserNotificationsForApartments(this)
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
            id = id,
        )
}

private const val LAND_PRICE_TOLERANCE = 0.15
