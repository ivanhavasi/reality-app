package cz.havasi.controller

import cz.havasi.model.User
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.repository.UserRepository
import cz.havasi.service.RealityService
import io.quarkus.logging.Log
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
internal class RealityController(
    private val realityService: RealityService,
    private val userRepository: UserRepository,
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(): String {
        Log.info("Searching estates")
        realityService.fetchAndSaveApartmentsForSale()

        return "Hello world"
    }

    // todo for testing only, remove later
    @GET
    public suspend fun test(): User {
        Log.info("INSIDE")
        val id = userRepository.save(CreateUserCommand(
            email = "test@mail.com",
            username = "test",
        ))

        println("WE GOT IT")
        println(id)
        val data = userRepository.getUserById(id)

        println(data.id)
        return data
    }

    @POST
    public suspend fun testPost(body: String): User {
        Log.info("INSIDE2")
        println(body)

        return userRepository.getUserById(body)
    }
}
