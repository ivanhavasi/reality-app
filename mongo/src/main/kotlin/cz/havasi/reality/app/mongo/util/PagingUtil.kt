package cz.havasi.reality.app.mongo.util

import cz.havasi.reality.app.model.util.Paging
import io.quarkus.mongodb.FindOptions
import org.bson.Document

internal fun Paging.toFindOptions() =
    FindOptions()
        .sort(Document(sortBy, sortDirection.value))
        .limit(limit)
        .skip(offset)
