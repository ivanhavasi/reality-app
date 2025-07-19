package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.reality.app.model.util.Paging

public interface ApartmentRepository {
    public suspend fun save(apartment: Apartment): String
    public suspend fun saveAll(apartments: List<Apartment>): List<String>
    public suspend fun bulkUpdateApartmentWithDuplicate(apartments: List<UpdateApartmentWithDuplicateCommand>)
    public suspend fun findAll(searchString: String?, transactionType: TransactionType, paging: Paging): List<Apartment>

    public suspend fun existsByIdOrFingerprint(id: String, fingerprint: String): Boolean
    public suspend fun findByIdOrFingerprint(id: String, fingerprint: String): List<Apartment>
}
