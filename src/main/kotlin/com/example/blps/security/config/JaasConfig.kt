package com.example.blps.security.config

import com.example.blps.security.jaas.VacancyLoginModule
import com.example.blps.security.service.XmlUserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.jaas.AuthorityGranter
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider
import org.springframework.security.authentication.jaas.JaasNameCallbackHandler
import org.springframework.security.authentication.jaas.JaasPasswordCallbackHandler
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.Principal
import javax.security.auth.login.AppConfigurationEntry
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag

@Configuration
class JaasConfig(
    private val xmlUserService: XmlUserService
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun jaasConfiguration(): InMemoryConfiguration {
        VacancyLoginModule.xmlUserService = xmlUserService
        VacancyLoginModule.passwordEncoder = passwordEncoder()

        val configEntries = arrayOf(
            AppConfigurationEntry(
                VacancyLoginModule::class.java.name,
                LoginModuleControlFlag.REQUIRED,
                emptyMap<String, Any>()
            )
        )

        val configMap = mapOf("SPRINGSECURITY" to configEntries)
        return InMemoryConfiguration(configMap)
    }

    @Bean
    fun jaasAuthenticationProvider(): DefaultJaasAuthenticationProvider {
        val provider = DefaultJaasAuthenticationProvider()

        provider.setConfiguration(jaasConfiguration())

        provider.setCallbackHandlers(arrayOf(
            JaasNameCallbackHandler(),
            JaasPasswordCallbackHandler()
        ))

        provider.setAuthorityGranters(arrayOf(
            VacancyAuthorityGranter()
        ))

        provider.setLoginContextName("SPRINGSECURITY")

        return provider
    }

    class VacancyAuthorityGranter : AuthorityGranter {
        override fun grant(principal: Principal): Set<String> {
            if (principal is VacancyLoginModule.VacancyRolePrincipal) {
                val roleName = principal.name
                val authorities = mutableSetOf("ROLE_$roleName")

                when (roleName) {
                    "HR_MANAGER" -> {
                        authorities.add("VACANCY_CREATE_EDIT")
                        authorities.add("VACANCY_PUBLISH")
                        authorities.add("VACANCY_VIEW_ALL")
                        authorities.add("VACANCY_VIEW_PUBLIC")
                        authorities.add("PAYMENT_PROCESS")
                    }
                    "CANDIDATE" -> {
                        authorities.add("VACANCY_VIEW_PUBLIC")
                    }
                }

                return authorities
            }
            return emptySet()
        }
    }
}