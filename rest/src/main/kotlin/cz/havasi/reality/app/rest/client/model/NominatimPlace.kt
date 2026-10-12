package cz.havasi.reality.app.rest.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.runtime.annotations.RegisterForReflection

// Nominatim sends coordinates as strings
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class NominatimPlace(
    @JsonProperty("lat")
    internal val latitude: String,
    @JsonProperty("lon")
    internal val longitude: String,
    @JsonProperty("display_name")
    internal val displayName: String,
)
