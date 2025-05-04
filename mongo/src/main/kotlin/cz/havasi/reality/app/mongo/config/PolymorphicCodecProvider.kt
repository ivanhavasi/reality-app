package cz.havasi.reality.app.mongo.config

import cz.havasi.reality.app.mongo.entity.DiscordWebhookNotificationEntity
import cz.havasi.reality.app.mongo.entity.EmailNotificationEntity
import cz.havasi.reality.app.mongo.entity.NotificationEntity
import cz.havasi.reality.app.mongo.entity.WebhookNotificationEntity
import jakarta.enterprise.context.ApplicationScoped
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.pojo.PojoCodecProvider

@ApplicationScoped
internal class PolymorphicCodecProvider : CodecProvider {
    private val polymorphicClasses = mapOf(
        NotificationEntity::class.java.name to listOf(
            EmailNotificationEntity::class.java,
            WebhookNotificationEntity::class.java,
            DiscordWebhookNotificationEntity::class.java,
        ),
    )
    private val pojoCodecProvider = PojoCodecProvider.builder()
        .automatic(true)
        .register(
            NotificationEntity::class.java,
            EmailNotificationEntity::class.java,
            WebhookNotificationEntity::class.java,
            DiscordWebhookNotificationEntity::class.java,
        )
        .build()

    override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        if (polymorphicClasses.containsKey(clazz.name)) {
            return pojoCodecProvider.get(clazz, registry)
        }
        polymorphicClasses.values.flatten().forEach {
            if (clazz.name == it.name) {
                return pojoCodecProvider.get(clazz, registry)
            }
        }
        return null
    }
}
