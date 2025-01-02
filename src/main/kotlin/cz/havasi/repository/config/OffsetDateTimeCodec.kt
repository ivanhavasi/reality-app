import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

public class OffsetDateTimeCodec : Codec<OffsetDateTime> {
    override fun encode(writer: BsonWriter, value: OffsetDateTime, encoderContext: EncoderContext) {
        writer.writeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): OffsetDateTime =
        OffsetDateTime.parse(reader.readString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    override fun getEncoderClass(): Class<OffsetDateTime> = OffsetDateTime::class.java
}
