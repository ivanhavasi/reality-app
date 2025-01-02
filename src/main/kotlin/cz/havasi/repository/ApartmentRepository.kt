package cz.havasi.repository

import cz.havasi.model.Apartment
import org.bson.types.ObjectId

public interface ApartmentRepository {
    public suspend fun save(apartment: Apartment): String
    public suspend fun saveAll(apartments: List<Apartment>): List<ObjectId>

    public suspend fun existsByIdOrFingerprint(id: String, fingerprint: String): Boolean
}
