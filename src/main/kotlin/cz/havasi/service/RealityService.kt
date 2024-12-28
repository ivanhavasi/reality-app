package cz.havasi.service

import cz.havasi.model.BuildingType
import cz.havasi.model.GetEstatesCommand
import cz.havasi.model.TransactionType
import cz.havasi.repository.ApartmentRepository
import io.quarkus.arc.All
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

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
        val apartments = provider.getEstates(GetEstatesCommand(
            type = BuildingType.APARTMENT,
            transaction = TransactionType.SALE,
        ))

        val filteredApartments = apartments
            .filter { !apartmentRepository.existsByIdOrFingerprint(it.id, it.fingerprint) } // todo, just update date in already existant apartments

        apartmentRepository.saveAll(filteredApartments) // todo check if all have been saved
    }
}
