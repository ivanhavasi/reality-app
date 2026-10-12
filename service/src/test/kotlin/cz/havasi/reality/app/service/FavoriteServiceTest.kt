package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Favorite
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.repository.FavoriteRepository
import kotlinx.coroutines.test.runTest
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class FavoriteServiceTest {
    private val apartments = FakeApartmentRepository()
    private val favorites = FakeFavoriteRepository()
    private val service = FavoriteService(favorites, apartments)

    private suspend fun stored(vararg ids: String) =
        ids.forEach { apartments.save(apartment(it, ProviderType.SREALITY, 50.0, 6_000_000.0)) }

    @Test
    fun `starred listings come back newest first`() = runTest {
        stored("first", "second")
        service.star("user", "first")
        service.star("user", "second")

        assertEquals(listOf("second", "first"), service.getFavorites("user").map { it.listing.id })
    }

    @Test
    fun `listing no longer stored is left out`() = runTest {
        stored("kept")
        service.star("user", "kept")
        favorites.save("user", "gone")

        assertEquals(listOf("kept"), service.getFavorites("user").map { it.listing.id })
    }

    @Test
    fun `unknown listing can't be starred`() = runTest {
        assertFailsWith<NoSuchElementException> { service.star("user", "missing") }
        assertEquals(emptyList(), service.getFavorites("user"))
    }

    @Test
    fun `each user sees and removes only their own favorites`() = runTest {
        stored("shared")
        service.star("user", "shared")
        service.star("other", "shared")

        service.unstar("user", "shared")

        assertEquals(emptyList(), service.getFavorites("user"))
        assertEquals(listOf("shared"), service.getFavorites("other").map { it.listing.id })
    }
}

private class FakeFavoriteRepository : FavoriteRepository {
    private val favorites = mutableListOf<Pair<String, Favorite>>()
    private var clock = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, UTC)

    override suspend fun save(userId: String, listingId: String) {
        if (favorites.none { (user, favorite) -> user == userId && favorite.listingId == listingId }) {
            clock = clock.plusMinutes(1)
            favorites += userId to Favorite(listingId, clock)
        }
    }

    override suspend fun delete(userId: String, listingId: String) {
        favorites.removeIf { (user, favorite) -> user == userId && favorite.listingId == listingId }
    }

    override suspend fun findByUser(userId: String): List<Favorite> =
        favorites.filter { it.first == userId }.map { it.second }.sortedByDescending { it.starredAt }
}
