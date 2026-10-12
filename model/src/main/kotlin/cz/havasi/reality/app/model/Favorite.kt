package cz.havasi.reality.app.model

import io.quarkus.runtime.annotations.RegisterForReflection
import java.time.OffsetDateTime

@RegisterForReflection
public data class Favorite(
    val listingId: String,
    val starredAt: OffsetDateTime,
)

@RegisterForReflection
public data class FavoriteListing(
    val listing: Apartment,
    val starredAt: OffsetDateTime,
)
