package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.model.type.ProviderType.BEZREALITKY
import cz.havasi.reality.app.model.type.ProviderType.IDNES
import cz.havasi.reality.app.model.type.ProviderType.SREALITY
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RealEstateServiceTest {
    private val batch = mutableListOf<Apartment>()
    private val fixture = RealEstateServiceFixture(
        listOf(FakeProvider("portal") { page -> if (page == 0) batch.toList() else emptyList() }),
    )
    private val repository get() = fixture.repository

    // one sighting per fetch: listings within one page can't see each other before they are saved
    private suspend fun see(vararg apartments: Apartment) {
        batch.clear()
        batch += apartments
        val type = apartments.first().mainCategory
        fixture.service.fetchAndSaveRealEstate(
            type,
            TransactionType.SALE,
            LandSubCategory.BUILDING_PLOT.takeIf { type == BuildingType.LAND },
        )
    }

    private fun duplicateIdsOf(id: String) = repository.byId(id)!!.duplicates.map { it.id }

    @Nested
    inner class LandDedup {
        @Test
        fun `plots of different area on the same portal are both saved`() = runTest {
            see(land("s1", SREALITY, 968.0, 9_000_000.0))
            see(land("s2", SREALITY, 991.0, 9_000_000.0))

            assertEquals(listOf("s1", "s2"), repository.stored.map { it.id })
            assertEquals(listOf("s1", "s2"), fixture.notifiedIds)
        }

        @Test
        fun `same area on the same portal with different ids are both saved`() = runTest {
            see(land("s1", SREALITY, 954.0, 9_000_000.0))
            see(land("s2", SREALITY, 954.0, 9_000_000.0))

            assertEquals(listOf("s1", "s2"), repository.stored.map { it.id })
            assertTrue(repository.stored.all { it.duplicates.isEmpty() })
        }

        @Test
        fun `same plot on another portal is stored as a duplicate with its id`() = runTest {
            see(land("s1", SREALITY, 954.0, 10_000_000.0, "Satalice"))
            see(land("b1", BEZREALITKY, 954.0, 9_900_000.0, "Satalice"))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(listOf("b1"), duplicateIdsOf("s1"))
            assertEquals(listOf("s1", "s1"), fixture.notifiedIds)
        }

        @Test
        fun `known duplicate seen again is neither saved nor notified`() = runTest {
            see(land("s1", SREALITY, 954.0, 10_000_000.0))
            see(land("b1", BEZREALITKY, 954.0, 9_900_000.0))
            see(land("b1", BEZREALITKY, 954.0, 9_900_000.0))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(listOf("b1"), duplicateIdsOf("s1"))
            assertEquals(2, fixture.notifiedIds.size)
        }

        @Test
        fun `different plot with the same area on a portal that already has a duplicate is saved as new`() = runTest {
            see(land("s1", SREALITY, 954.0, 10_000_000.0))
            see(land("b1", BEZREALITKY, 954.0, 9_900_000.0))
            see(land("b2", BEZREALITKY, 954.0, 10_000_000.0))

            assertEquals(listOf("s1", "b2"), repository.stored.map { it.id })
            assertEquals(listOf("b1"), duplicateIdsOf("s1"))
        }

        @Test
        fun `two listings of one portal on the same page attach at most one duplicate to a plot`() = runTest {
            see(land("i1", IDNES, 594.0, 8_990_000.0, "Benice"))
            see(
                land("s1", SREALITY, 594.0, 8_390_000.0, "Benice"),
                land("s2", SREALITY, 594.0, 8_990_000.0, "Benice"),
            )

            assertEquals(listOf("i1", "s2"), repository.stored.map { it.id })
            assertEquals(listOf("s1"), duplicateIdsOf("i1"))
        }

        @Test
        fun `known duplicate stays with its original when a newer plot has the same area`() = runTest {
            see(land("s1", SREALITY, 954.0, 10_000_000.0))
            see(land("b1", BEZREALITKY, 954.0, 9_900_000.0))
            see(land("s2", SREALITY, 954.0, 10_000_000.0))
            see(land("b1", BEZREALITKY, 954.0, 9_900_000.0))

            assertEquals(listOf("s1", "s2"), repository.stored.map { it.id })
            assertEquals(listOf("b1"), duplicateIdsOf("s1"))
            assertEquals(emptyList(), duplicateIdsOf("s2"))
        }

        @Test
        fun `known duplicate whose area changed is recognised by its id`() = runTest {
            see(land("s1", SREALITY, 954.0, 10_000_000.0))
            see(land("b1", BEZREALITKY, 954.0, 9_900_000.0))
            see(land("b1", BEZREALITKY, 955.0, 9_900_000.0))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(listOf("b1"), duplicateIdsOf("s1"))
        }

        @Test
        fun `price on request is stored with zero price per m2 and merges with a priced copy`() = runTest {
            see(land("s1", SREALITY, 1_200.0, 0.0, "Radotín"))
            see(land("i1", IDNES, 1_200.0, 8_000_000.0, "Radotín"))

            assertEquals(0.0, repository.byId("s1")!!.pricePerM2)
            assertEquals(listOf("i1"), duplicateIdsOf("s1"))
        }
    }

    @Nested
    inner class LandGuard {
        @Test
        fun `Satalice vs Modrany with the same area are not merged`() = runTest {
            see(land("s1", SREALITY, 954.0, 12_500_000.0, "Satalice"))
            see(land("i1", IDNES, 954.0, 12_400_000.0, "Modřany"))

            assertEquals(listOf("s1", "i1"), repository.stored.map { it.id })
        }

        @Test
        fun `Kyje vs Modrany with the same area are not merged`() = runTest {
            see(land("s1", SREALITY, 890.0, 8_900_000.0, "Kyje"))
            see(land("i1", IDNES, 890.0, 9_000_000.0, "Praha-Modřany"))

            assertEquals(listOf("s1", "i1"), repository.stored.map { it.id })
        }

        @Test
        fun `price gap over 15 percent is not merged`() = runTest {
            see(land("s1", SREALITY, 890.0, 8_900_000.0))
            see(land("i1", IDNES, 890.0, 12_000_000.0))

            assertEquals(listOf("s1", "i1"), repository.stored.map { it.id })
        }

        @Test
        fun `true pair 1_3 percent apart in price is merged`() = runTest {
            see(land("s1", SREALITY, 1_418.0, 10_000_000.0, "Radotín"))
            see(land("i1", IDNES, 1_418.0, 9_870_000.0, "Praha-Radotín"))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(listOf("i1"), duplicateIdsOf("s1"))
        }

        @Test
        fun `one side without a city part is merged`() = runTest {
            see(land("s1", SREALITY, 1_418.0, 10_000_000.0, "Kunratice"))
            see(land("b1", BEZREALITKY, 1_418.0, 10_100_000.0, null))
            see(land("i1", IDNES, 1_418.0, 10_000_000.0, "Praha 4"))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(listOf("b1", "i1"), duplicateIdsOf("s1"))
        }
    }

    @Nested
    inner class FetchLoop {
        @Test
        fun `land scans every page and stops at the first empty fetch`() = runTest {
            val known = land("s1", SREALITY, 954.0, 10_000_000.0)
            val provider = FakeProvider("sreality") { page -> if (page < 3) listOf(known) else emptyList() }
            val fixture = RealEstateServiceFixture(listOf(provider))

            fixture.service.fetchAndSaveRealEstate(BuildingType.LAND, TransactionType.SALE, LandSubCategory.BUILDING_PLOT)

            assertEquals(4, provider.commands.size)
            assertEquals(listOf("s1"), fixture.notifiedIds)
        }

        @Test
        fun `land fetches at most 12 pages and passes the subtype`() = runTest {
            val provider = FakeProvider("sreality") { page -> listOf(land("s$page", SREALITY, 500.0 + page, 5_000_000.0)) }
            val fixture = RealEstateServiceFixture(listOf(provider))

            fixture.service.fetchAndSaveRealEstate(BuildingType.LAND, TransactionType.SALE, LandSubCategory.BUILDING_PLOT)

            assertEquals(12, provider.commands.size)
            assertEquals((0 until 12).map { it * 22 }, provider.commands.map { it.offset })
            assertTrue(provider.commands.all { it.type == BuildingType.LAND && it.landSubCategory == LandSubCategory.BUILDING_PLOT })
        }

        @Test
        fun `apartments still stop at the first page with nothing new`() = runTest {
            val known = apartment("a1", SREALITY, 65.0, 7_000_000.0)
            val provider = FakeProvider("sreality") { listOf(known) }
            val fixture = RealEstateServiceFixture(listOf(provider))

            fixture.service.fetchAndSaveRealEstate(BuildingType.APARTMENT, TransactionType.SALE)

            assertEquals(2, provider.commands.size)
            assertTrue(provider.commands.all { it.landSubCategory == null })
        }

        @Test
        fun `apartments fetch at most 5 pages`() = runTest {
            val provider = FakeProvider("sreality") { page -> listOf(apartment("a$page", SREALITY, 65.0, 7_000_000.0, street = "Street $page")) }
            val fixture = RealEstateServiceFixture(listOf(provider))

            fixture.service.fetchAndSaveRealEstate(BuildingType.APARTMENT, TransactionType.SALE)

            assertEquals(5, provider.commands.size)
        }

        @Test
        fun `land providers run sequentially and merge a plot listed on both in the same run`() = runTest {
            val events = mutableListOf<String>()
            val sreality = FakeProvider("sreality", events, 1_000) { page ->
                if (page == 0) listOf(land("s1", SREALITY, 954.0, 10_000_000.0)) else emptyList()
            }
            val bezrealitky = FakeProvider("bezrealitky", events, 1_000) { page ->
                if (page == 0) listOf(land("b1", BEZREALITKY, 954.0, 9_900_000.0)) else emptyList()
            }
            val fixture = RealEstateServiceFixture(listOf(sreality, bezrealitky))

            fixture.service.fetchAndSaveRealEstate(BuildingType.LAND, TransactionType.SALE, LandSubCategory.BUILDING_PLOT)

            assertTrue(events.indexOfLast { it.startsWith("sreality") } < events.indexOfFirst { it.startsWith("bezrealitky") })
            assertEquals(listOf("s1"), fixture.repository.stored.map { it.id })
            assertEquals(listOf("b1"), fixture.repository.byId("s1")!!.duplicates.map { it.id })
        }

        @Test
        fun `apartment providers still run in parallel`() = runTest {
            val events = mutableListOf<String>()
            val sreality = FakeProvider("sreality", events, 1_000) { page ->
                if (page == 0) listOf(apartment("s1", SREALITY, 65.0, 7_000_000.0)) else emptyList()
            }
            val idnes = FakeProvider("idnes", events, 1_000) { page ->
                if (page == 0) listOf(apartment("i1", IDNES, 80.0, 9_000_000.0, street = "Jiná")) else emptyList()
            }
            val fixture = RealEstateServiceFixture(listOf(sreality, idnes))

            fixture.service.fetchAndSaveRealEstate(BuildingType.APARTMENT, TransactionType.SALE)

            assertTrue(events.indexOfFirst { it.startsWith("idnes") } < events.indexOfLast { it.startsWith("sreality") })
        }

        @Test
        fun `a failing land provider does not stop the next one`() = runTest {
            val failing = FakeProvider("sreality") { error("boom") }
            val idnes = FakeProvider("idnes") { page -> if (page == 0) listOf(land("i1", IDNES, 700.0, 7_000_000.0)) else emptyList() }
            val fixture = RealEstateServiceFixture(listOf(failing, idnes))

            fixture.service.fetchAndSaveRealEstate(BuildingType.LAND, TransactionType.SALE, LandSubCategory.BUILDING_PLOT)

            assertEquals(listOf("i1"), fixture.repository.stored.map { it.id })
        }
    }

    @Nested
    inner class ApartmentDedupUnchanged {
        @Test
        fun `apartment from another portal with a similar size is stored as a duplicate`() = runTest {
            see(apartment("s1", SREALITY, 65.0, 7_000_000.0))
            see(apartment("i1", IDNES, 66.0, 7_500_000.0))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(listOf("i1"), duplicateIdsOf("s1"))
        }

        @Test
        fun `apartment with the same fingerprint but a size over 5 percent apart is new`() = runTest {
            see(apartment("s1", SREALITY, 65.0, 7_000_000.0))
            see(apartment("i1", IDNES, 70.0, 7_000_000.0))

            assertEquals(listOf("s1", "i1"), repository.stored.map { it.id })
        }

        @Test
        fun `apartment on the same portal with a new id is merged and kept only when cheaper`() = runTest {
            see(apartment("s1", SREALITY, 65.0, 7_000_000.0))
            see(apartment("s2", SREALITY, 65.0, 7_000_000.0))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(emptyList(), duplicateIdsOf("s1"))

            see(apartment("s3", SREALITY, 65.0, 6_500_000.0))

            assertEquals(listOf("s3"), duplicateIdsOf("s1"))
        }

        @Test
        fun `apartment re-sighting by id is not saved again`() = runTest {
            see(apartment("s1", SREALITY, 65.0, 7_000_000.0))
            see(apartment("s1", SREALITY, 65.0, 7_000_000.0))

            assertEquals(listOf("s1"), repository.stored.map { it.id })
            assertEquals(listOf("s1"), fixture.notifiedIds)
        }
    }
}
