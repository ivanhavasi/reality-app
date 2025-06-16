package cz.havasi.reality.app.model.type

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public enum class NotificationType {
    EMAIL,
    DISCORD,
    WEBHOOK,
}