package cz.havasi.rest.controller.util

import org.jboss.resteasy.reactive.RestResponse

internal fun <T> T.wrapToNoContent() =
    RestResponse.noContent<Nothing>()

internal fun <T> T.wrapToOk() =
    RestResponse.ok(this)
