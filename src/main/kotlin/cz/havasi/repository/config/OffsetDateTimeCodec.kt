package cz.havasi.repository.config

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

public class OffsetDateTimeCodec : Codec<OffsetDateTime> {
    override fun encode(writer: BsonWriter, value: OffsetDateTime, encoderContext: EncoderContext): Unit =
        writer.writeDateTime(value.toInstant().toEpochMilli())

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): OffsetDateTime =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(reader.readDateTime()), UTC)

    override fun getEncoderClass(): Class<OffsetDateTime> = OffsetDateTime::class.java
}
