package cz.havasi.reality.app.rest.controller.model

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
internal data class LoginRequest(
    val code: String,
)

// the refresh token stays in an HttpOnly cookie, the browser only sees the short-lived access token
@RegisterForReflection
internal data class Session(
    val accessToken: String,
    val expiresIn: Long,
)
