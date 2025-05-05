package cz.havasi.reality.app.rest.security

import cz.havasi.reality.app.model.type.UserRole
import jakarta.annotation.security.RolesAllowed
import kotlin.annotation.AnnotationRetention.RUNTIME

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(RUNTIME)
@RolesAllowed(UserRole.USER_ROLE, UserRole.ADMIN_ROLE)

public annotation class RequireUserMatch(
    val value: String = "id",
    val pathParam: String = "userId",
)
