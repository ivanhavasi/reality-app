package cz.havasi.reality.app.rest.controller

import cz.havasi.reality.app.model.AdminJob
import cz.havasi.reality.app.model.AdminJobType
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.UserRole.Companion.ADMIN_ROLE
import cz.havasi.reality.app.rest.controller.util.wrapToOk
import cz.havasi.reality.app.service.AdminJobAlreadyRunningException
import cz.havasi.reality.app.service.AdminJobService
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.ClientErrorException
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.reactive.RestResponse

// jobs run in the background: starting one returns 202 right away, GET reports progress
@Path("/api/admin/jobs")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(ADMIN_ROLE)
internal open class AdminJobController(
    private val adminJobService: AdminJobService,
) {
    @GET
    open fun getJobs(): RestResponse<List<AdminJob>> = adminJobService.recentJobs().wrapToOk()

    // the same work as the scheduled scrape: newest pages of every portal, saves and notifies
    @POST
    @Path("/scrape")
    open fun startScrape(
        @DefaultValue("APARTMENT") @QueryParam("building") building: BuildingType,
        @DefaultValue("SALE") @QueryParam("transaction") transaction: TransactionType,
    ): RestResponse<AdminJob> = start(AdminJobType.SCRAPE, building, transaction)

    @POST
    @Path("/backfill-coordinates")
    open fun startCoordinatesBackfill(
        @DefaultValue("APARTMENT") @QueryParam("building") building: BuildingType,
        @DefaultValue("SALE") @QueryParam("transaction") transaction: TransactionType,
    ): RestResponse<AdminJob> = start(AdminJobType.BACKFILL_COORDINATES, building, transaction)

    private fun start(type: AdminJobType, building: BuildingType, transaction: TransactionType): RestResponse<AdminJob> =
        try {
            RestResponse.accepted(adminJobService.start(type, building, transaction))
        } catch (e: AdminJobAlreadyRunningException) {
            throw ClientErrorException(
                Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(mapOf("message" to e.message))
                    .build(),
            )
        }
}
