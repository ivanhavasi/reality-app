package cz.havasi.repository

import cz.havasi.model.Apartment
import cz.havasi.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.model.util.Paging
import org.bson.types.ObjectId

public interface ApartmentRepository {
    public suspend fun save(apartment: Apartment): String
    public suspend fun saveAll(apartments: List<Apartment>): List<ObjectId>
    public suspend fun bulkUpdateApartmentWithDuplicate(apartments: List<UpdateApartmentWithDuplicateCommand>)
    public suspend fun findAll(paging: Paging): List<Apartment>

    public suspend fun existsByIdOrFingerprint(id: String, fingerprint: String): Boolean
    public suspend fun findByIdOrFingerprint(id: String, fingerprint: String): List<Apartment>
}
