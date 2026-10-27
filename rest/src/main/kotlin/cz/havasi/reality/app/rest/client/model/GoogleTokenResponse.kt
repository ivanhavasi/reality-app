package cz.havasi.reality.app.rest.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GoogleTokenResponse(
    @JsonProperty("access_token")
    internal val accessToken: String,
    @JsonProperty("expires_in")
    internal val expiresIn: Long,
    @JsonProperty("refresh_token")
    internal val refreshToken: String? = null,
    @JsonProperty("id_token")
    internal val idToken: String? = null,
)
