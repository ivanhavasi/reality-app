package cz.havasi.config.security

import jakarta.annotation.security.RolesAllowed
import kotlin.annotation.AnnotationRetention.RUNTIME

@MustBeDocumented
@Target(AnnotationTarget.FUNCTION)
@Retention(RUNTIME)
@RolesAllowed("USER", "ADMIN")
public annotation class RequireUserMatch(
    val value: String = "id",
    val pathParam: String = "userId",
)
