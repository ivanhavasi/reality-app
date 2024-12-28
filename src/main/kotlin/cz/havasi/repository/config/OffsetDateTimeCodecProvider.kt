package cz.havasi.repository.config

import OffsetDateTimeCodec
import jakarta.enterprise.context.ApplicationScoped
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import java.time.OffsetDateTime

@ApplicationScoped
public class OffsetDateTimeCodecProvider : CodecProvider {
    public override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (clazz == OffsetDateTime::class.java) {
            return OffsetDateTimeCodec() as Codec<T>?
        }
        // return null when not a provider for the requested class
        return null
    }
}
