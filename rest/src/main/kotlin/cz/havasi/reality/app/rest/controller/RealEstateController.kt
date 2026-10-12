package cz.havasi.reality.app.rest.controller

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.model.type.UserRole.Companion.ADMIN_ROLE
import cz.havasi.reality.app.model.type.UserRole.Companion.USER_ROLE
import cz.havasi.reality.app.model.util.Paging
import cz.havasi.reality.app.model.util.SortDirection
import cz.havasi.reality.app.rest.controller.model.RealEstateMapPoint
import cz.havasi.reality.app.rest.controller.model.toMapPoint
import cz.havasi.reality.app.rest.controller.util.wrapToNoContent
import cz.havasi.reality.app.rest.controller.util.wrapToOk
import cz.havasi.reality.app.service.RealEstateService
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestResponse

@Path("/api/real-estates")
@Produces(MediaType.APPLICATION_JSON)
internal open class RealEstateController(
    private val realEstateService: RealEstateService,
) {
    @POST
    @RolesAllowed(ADMIN_ROLE)
    @Path("/process")
    open suspend fun processNewRealEstates(
        @DefaultValue("APARTMENT") @QueryParam("building") building: String,
        @DefaultValue("SALE") @QueryParam("transaction") transaction: String,
        @QueryParam("landSubCategory") landSubCategory: String?,
    ): RestResponse<Nothing> {
        val buildingType = building.toBuildingType()
        return realEstateService
            .fetchAndSaveRealEstate(buildingType, transaction.toTransactionType(), resolveLandSubCategory(buildingType, landSubCategory))
            .wrapToNoContent()
    }

    @GET
    @RolesAllowed(USER_ROLE)
    open suspend fun findRealEstates(
        @DefaultValue("0") @QueryParam("offset") offset: Int,
        @DefaultValue("20") @QueryParam("limit") limit: Int,
        @DefaultValue("DESC") @QueryParam("sortDirection") sortDirection: String,
        @DefaultValue("SALE") @QueryParam("transaction") transaction: String,
        @DefaultValue("APARTMENT") @QueryParam("building") building: String,
        @DefaultValue("0") @QueryParam("sizeMin") sizeMin: Int,
        @DefaultValue("1000000") @QueryParam("sizeMax") sizeMax: Int,
        @DefaultValue("0") @QueryParam("priceMin") priceMin: Int,
        @DefaultValue("1000000000") @QueryParam("priceMax") priceMax: Int,
        @QueryParam("search") searchString: String? = null,
        @QueryParam("subCategory") subCategories: List<String>,
    ): RestResponse<List<Apartment>> =
        realEstateService.findRealEstates(
            findCommand(
                searchString, transaction, building, sizeMin, sizeMax, priceMin, priceMax, subCategories,
                Paging(
                    offset = offset.coerceAtLeast(0),
                    limit = limit.coerceIn(10, 20),
                    sortDirection = sortDirection.toSortDirection(),
                ),
            ),
        )
            .wrapToOk()

    // same filters as findRealEstates, but only listings with coordinates and only the fields a map pin needs
    @GET
    @Path("/map")
    @RolesAllowed(USER_ROLE)
    open suspend fun findRealEstatesForMap(
        @DefaultValue("$MAX_MAP_POINTS") @QueryParam("limit") limit: Int,
        @DefaultValue("DESC") @QueryParam("sortDirection") sortDirection: String,
        @DefaultValue("SALE") @QueryParam("transaction") transaction: String,
        @DefaultValue("APARTMENT") @QueryParam("building") building: String,
        @DefaultValue("0") @QueryParam("sizeMin") sizeMin: Int,
        @DefaultValue("1000000") @QueryParam("sizeMax") sizeMax: Int,
        @DefaultValue("0") @QueryParam("priceMin") priceMin: Int,
        @DefaultValue("1000000000") @QueryParam("priceMax") priceMax: Int,
        @QueryParam("search") searchString: String? = null,
        @QueryParam("subCategory") subCategories: List<String>,
    ): RestResponse<List<RealEstateMapPoint>> =
        realEstateService.findRealEstates(
            findCommand(
                searchString, transaction, building, sizeMin, sizeMax, priceMin, priceMax, subCategories,
                Paging(
                    limit = limit.coerceIn(1, MAX_MAP_POINTS),
                    sortDirection = sortDirection.toSortDirection(),
                ),
                withCoordinates = true,
            ),
        )
            .map { it.toMapPoint() }
            .wrapToOk()

    // NOT SECURED EP
    @GET
    @Path("/{id}")
    open suspend fun getRealEstateById(
        @PathParam("id") id: String,
    ): RestResponse<Apartment> = realEstateService.getById(id).wrapToOk()

    private fun findCommand(
        searchString: String?,
        transaction: String,
        building: String,
        sizeMin: Int,
        sizeMax: Int,
        priceMin: Int,
        priceMax: Int,
        subCategories: List<String>,
        paging: Paging,
        withCoordinates: Boolean = false,
    ) = FindRealEstatesCommand(
        searchString,
        transaction.toTransactionType(),
        building.toBuildingType(),
        sizeMin,
        sizeMax,
        priceMin,
        priceMax,
        paging,
        subCategories,
        withCoordinates,
    )

    // an unfiltered land fetch can't be classified (Bezrealitky never reveals the subtype), and phase 1 scrapes building plots only
    private fun resolveLandSubCategory(buildingType: BuildingType, landSubCategory: String?): LandSubCategory? =
        when {
            buildingType != BuildingType.LAND -> null
            landSubCategory == null -> LandSubCategory.BUILDING_PLOT
            else -> LandSubCategory.fromValueOrNull(landSubCategory)
                ?: throw BadRequestException("Unknown landSubCategory $landSubCategory")
        }

    private fun String.toTransactionType() = when (this) {
        "SALE" -> TransactionType.SALE
        "RENT" -> TransactionType.RENT
        else -> TransactionType.SALE
    }

    private fun String.toBuildingType() = when (this) {
        "APARTMENT" -> BuildingType.APARTMENT
        "HOUSE" -> BuildingType.HOUSE
        "LAND" -> BuildingType.LAND
        else -> BuildingType.APARTMENT
    }

    private fun String.toSortDirection() = when (this) {
        "ASC" -> SortDirection.ASC
        "DESC" -> SortDirection.DESC
        else -> SortDirection.DESC
    }

    private companion object {
        // ~300 bytes per point, so a full response stays around 150 kB
        const val MAX_MAP_POINTS = 500
    }
}
