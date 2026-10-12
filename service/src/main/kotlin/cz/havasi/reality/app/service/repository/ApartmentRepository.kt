package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.MarketStatistics
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.command.GetStatisticsCommand
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand

public interface ApartmentRepository {
    public suspend fun save(apartment: Apartment): String
    public suspend fun saveAll(apartments: List<Apartment>): List<String>
    public suspend fun bulkUpdateApartmentWithDuplicate(apartments: List<UpdateApartmentWithDuplicateCommand>)
    // sets coordinates only where they are still missing; returns how many listings got them
    public suspend fun bulkUpdateLocations(apartments: List<Apartment>): Int
    public suspend fun findAll(command: FindRealEstatesCommand): List<Apartment>
    // newest first; only listings with a street, the others can't be located from their address
    public suspend fun findWithoutCoordinates(buildingType: BuildingType, transactionType: TransactionType): List<Apartment>

    public suspend fun existsByIdOrFingerprint(id: String, fingerprint: String): Boolean
    public suspend fun findByIdOrFingerprint(id: String, fingerprint: String): List<Apartment>

    public suspend fun getMarketStatistics(command: GetStatisticsCommand): List<MarketStatistics>
}
