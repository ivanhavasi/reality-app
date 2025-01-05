package cz.havasi.rest.controller.model

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
internal data class ResponseId(
    val id: String,
)
