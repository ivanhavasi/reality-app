package cz.havasi.reality.app.model.type

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public enum class UserRole {
    USER,
    ADMIN;

    public companion object {
        public const val USER_ROLE: String = "USER"
        public const val ADMIN_ROLE: String = "ADMIN"
    }
}
