package cz.havasi.reality.app.model

import io.quarkus.runtime.annotations.RegisterForReflection
import java.time.OffsetDateTime

@RegisterForReflection
public enum class AdminJobType {
    SCRAPE,
    BACKFILL_COORDINATES,
    IMPORT_PLACES,
}

@RegisterForReflection
public enum class AdminJobStatus {
    WAITING, // for the portal lock, held by a scheduled scrape
    RUNNING,
    SUCCEEDED,
    FAILED,
}

@RegisterForReflection
public data class AdminJob(
    val id: String,
    val type: AdminJobType,
    // null for the places import, which isn't about one kind of listing
    val buildingType: BuildingType?,
    val transactionType: TransactionType?,
    val status: AdminJobStatus,
    val createdAt: OffsetDateTime,
    val startedAt: OffsetDateTime? = null,
    val finishedAt: OffsetDateTime? = null,
    // scrape: new listings plus copies attached from another portal
    val savedListings: Int = 0,
    // backfill
    val pagesScanned: Int = 0,
    val listingsSeen: Int = 0,
    val coordinatesFilled: Int = 0,
    val addressesToLocate: Int = 0,
    val addressesChecked: Int = 0,
    val addressesLocated: Int = 0,
    // places import
    val placeCategoriesImported: Int = 0,
    val placesImported: Int = 0,
    val error: String? = null,
)
