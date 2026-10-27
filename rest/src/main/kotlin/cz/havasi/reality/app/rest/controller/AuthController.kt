package cz.havasi.reality.app.rest.controller

import cz.havasi.reality.app.model.GoogleTokens
import cz.havasi.reality.app.rest.controller.model.LoginRequest
import cz.havasi.reality.app.rest.controller.model.Session
import cz.havasi.reality.app.service.AuthService
import cz.havasi.reality.app.service.client.InvalidGoogleGrantException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.CookieParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.NewCookie
import org.jboss.resteasy.reactive.RestResponse
import java.time.Duration

// public: the browser calls these without its (possibly expired) access token
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
internal open class AuthController(
    private val authService: AuthService,
) {
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    open suspend fun login(request: LoginRequest): RestResponse<Session> {
        val tokens = try {
            authService.signIn(request.code)
        } catch (_: InvalidGoogleGrantException) {
            return unauthorized()
        }
        val response = RestResponse.ResponseBuilder.ok(tokens.toSession())
        tokens.refreshToken?.let { response.cookie(refreshCookie(it, COOKIE_MAX_AGE)) }
        return response.build()
    }

    @POST
    @Path("/refresh")
    open suspend fun refresh(@CookieParam(REFRESH_COOKIE) refreshToken: String?): RestResponse<Session> {
        if (refreshToken == null) return unauthorized()
        return try {
            RestResponse.ok(authService.refresh(refreshToken).toSession())
        } catch (_: InvalidGoogleGrantException) {
            unauthorized()
        }
    }

    @POST
    @Path("/logout")
    open fun logout(): RestResponse<Nothing> =
        RestResponse.ResponseBuilder.noContent<Nothing>().cookie(refreshCookie("", Duration.ZERO)).build()

    private fun GoogleTokens.toSession() = Session(accessToken, expiresInSeconds)

    // also clears the cookie, so the browser stops offering a refresh token Google no longer accepts
    private fun unauthorized(): RestResponse<Session> =
        RestResponse.ResponseBuilder.create<Session>(RestResponse.Status.UNAUTHORIZED)
            .cookie(refreshCookie("", Duration.ZERO))
            .build()

    private fun refreshCookie(value: String, maxAge: Duration): NewCookie =
        NewCookie.Builder(REFRESH_COOKIE)
            .value(value)
            .path("/api/auth")
            .maxAge(maxAge.seconds.toInt())
            .httpOnly(true)
            .secure(true)
            .sameSite(NewCookie.SameSite.STRICT)
            .build()

    private companion object {
        const val REFRESH_COOKIE = "reality_refresh"
        val COOKIE_MAX_AGE: Duration = Duration.ofDays(180)
    }
}
