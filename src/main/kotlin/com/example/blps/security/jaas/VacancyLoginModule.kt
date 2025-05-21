package com.example.blps.security.jaas

import com.example.blps.security.service.XmlUserService
import org.springframework.security.crypto.password.PasswordEncoder
import javax.security.auth.Subject
import javax.security.auth.callback.*
import javax.security.auth.login.LoginException
import javax.security.auth.spi.LoginModule
import java.security.Principal

class VacancyLoginModule : LoginModule {
    private var subject: Subject? = null
    private var callbackHandler: CallbackHandler? = null
    private var sharedState: Map<String, *>? = null
    private var options: Map<String, *>? = null

    private var succeeded = false
    private var commitSucceeded = false

    private var username: String? = null
    private var userPrincipal: VacancyUserPrincipal? = null
    private var userRoles: List<String>? = null

    // Эти сервисы будут внедрены через статический контекст, поскольку JAAS не поддерживает DI
    companion object {
        lateinit var xmlUserService: XmlUserService
        lateinit var passwordEncoder: PasswordEncoder
    }

    override fun initialize(
        subject: Subject, callbackHandler: CallbackHandler,
        sharedState: Map<String, *>, options: Map<String, *>
    ) {
        this.subject = subject
        this.callbackHandler = callbackHandler
        this.sharedState = sharedState
        this.options = options
    }

    override fun login(): Boolean {
        val nameCallback = NameCallback("username")
        val passwordCallback = PasswordCallback("password", false)

        try {
            callbackHandler?.handle(arrayOf(nameCallback, passwordCallback))

            username = nameCallback.name
            val password = String(passwordCallback.password)

            passwordCallback.clearPassword()

            val user = xmlUserService.findByUsername(username!!)
            if (user != null && passwordEncoder.matches(password, user.password)) {
                userRoles = user.roles
                succeeded = true
                return true
            }

            throw LoginException("Authentication failed")
        } catch (e: LoginException) {
            throw e
        } catch (e: Exception) {
            throw LoginException("Authentication failed: " + e.message)
        }
    }

    override fun commit(): Boolean {
        if (!succeeded) {
            return false
        }

        userPrincipal = VacancyUserPrincipal(username!!)

        if (!subject!!.principals.contains(userPrincipal)) {
            subject!!.principals.add(userPrincipal)

            userRoles?.forEach { role ->
                subject!!.principals.add(VacancyRolePrincipal(role))
            }
        }

        commitSucceeded = true
        return true
    }

    override fun abort(): Boolean {
        if (!succeeded) {
            return false
        }

        if (commitSucceeded) {
            logout()
        } else {
            username = null
            userPrincipal = null
            userRoles = null
        }

        succeeded = false
        commitSucceeded = false
        return true
    }

    override fun logout(): Boolean {
        subject!!.principals.remove(userPrincipal)
        userRoles?.forEach { role ->
            subject!!.principals.removeIf { it is VacancyRolePrincipal && it.name == role }
        }

        username = null
        userPrincipal = null
        userRoles = null

        succeeded = false
        commitSucceeded = false
        return true
    }

    class VacancyUserPrincipal(private val name: String) : Principal {
        override fun getName(): String = name
        override fun equals(other: Any?): Boolean = other is VacancyUserPrincipal && other.name == name
        override fun hashCode(): Int = name.hashCode()
        override fun toString(): String = "VacancyUserPrincipal[$name]"
    }

    class VacancyRolePrincipal(private val name: String) : Principal {
        override fun getName(): String = name
        override fun equals(other: Any?): Boolean = other is VacancyRolePrincipal && other.name == name
        override fun hashCode(): Int = name.hashCode()
        override fun toString(): String = "VacancyRolePrincipal[$name]"
    }
}