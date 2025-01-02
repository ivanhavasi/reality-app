package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.BuildingType
import cz.havasi.model.command.GetEstatesCommand
import cz.havasi.model.TransactionType
import cz.havasi.repository.ApartmentRepository
import io.quarkus.arc.All
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import kotlin.random.Random

@ApplicationScoped
internal class RealityService(
    private val apartmentRepository: ApartmentRepository,
    @All private val estateProviders: MutableList<EstatesProvider>,
) {

    init {
        Log.debug("Estate providers: $estateProviders")
    }

    suspend fun fetchAndSaveApartmentsForSale() {
        Log.info("Fetching and saving apartments for sale")
        estateProviders.forEach { fetchAndSaveApartmentsForProvider(it) }
    }

    private suspend fun fetchAndSaveApartmentsForProvider(provider: EstatesProvider) {
        val numberOfApartments = 22
        val maxCalls = 5

        for (i in 0 until maxCalls) {
            Log.info("Fetching apartments for provider $provider, call $i")
            val wereSaved = provider
                .getApartments(i * numberOfApartments, numberOfApartments)
                .saveApartments()

            if (!wereSaved) {
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
            )
        )

        val filteredApartments = apartments
            .filter {
                !apartmentRepository.existsByIdOrFingerprint(
                    it.id,
                    it.fingerprint
                )
            } // todo, just update date in already existant apartments?

        Log.info("Saving ${filteredApartments.size} apartments (vs ${apartments.size} fetched)")
        return filteredApartments
    }

    private suspend fun List<Apartment>.saveApartments(): Boolean =
        if (isNotEmpty()) {
            apartmentRepository.saveAll(this) // todo check if all have been saved
            true
        } else {
            false
        }
}
