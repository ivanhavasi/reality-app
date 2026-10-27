package cz.havasi.reality.app.mongo

internal object DatabaseNames {
    const val DB_NAME = "reality"
    const val APARTMENT_COLLECTION_NAME = "apartments"
    const val USER_COLLECTION_NAME = "users"
    const val USER_NOTIFICATION_COLLECTION_NAME = "notifications"
    const val SENT_NOTIFICATION_COLLECTION_NAME = "sentNotifications"
    const val GEOCODING_CACHE_COLLECTION_NAME = "geocodingCache"
    const val FAVORITE_COLLECTION_NAME = "favoriteListings"
    const val PLACE_COLLECTION_NAME = "places"
    const val GOOGLE_REFRESH_TOKEN_COLLECTION_NAME = "googleRefreshTokens"
}
