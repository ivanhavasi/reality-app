package cz.havasi.config.security

import cz.havasi.model.User
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.enum.UserRole
import cz.havasi.repository.UserRepository
import io.quarkus.logging.Log
import io.quarkus.oidc.UserInfo
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@ApplicationScoped
public class MongoIdentityAugmentor(
    private val userRepository: UserRepository,
) : SecurityIdentityAugmentor {
    override fun augment(
        p0: SecurityIdentity,
        p1: AuthenticationRequestContext,
    ): Uni<SecurityIdentity> {
        val userInfo = getUserInfo(p0) ?: return Uni.createFrom().item(p0)
        val securityIdentity = updateSecurityIdentity(userInfo, p0)

        return Uni.createFrom().item(securityIdentity)
    }

    private fun getUserInfo(securityIdentity: SecurityIdentity) =
        try {
            securityIdentity.attributes["userinfo"] as UserInfo
        } catch (_: Exception) {
            Log.error("Could not get userinfo from security identity")
            null
        }

    private fun updateSecurityIdentity(userInfo: UserInfo, securityIdentity: SecurityIdentity) =
        runBlocking(Dispatchers.IO) {
            val user = userRepository.getUserByEmailOrNull(userInfo.email)
                ?: createUser(userInfo) // if user does not exist, create it
            val roles = user.roles.map(UserRole::name).toSet()
            val builder: QuarkusSecurityIdentity.Builder = QuarkusSecurityIdentity.builder(securityIdentity)

            builder.addRoles(roles).build()
        }

    private suspend fun createUser(userInfo: UserInfo): User {
        val id = userRepository.save(
            CreateUserCommand(
                email = userInfo.email,
                username = userInfo.email.split("@").first(), // first part of email is username
                googleId = userInfo.subject, // google unique id
            ),
        )

        return userRepository.getUserById(id)
    }
}
