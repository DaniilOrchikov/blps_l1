package com.example.blps.security.config

import com.example.blps.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jaasAuthenticationProvider: DefaultJaasAuthenticationProvider,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    @Bean
    fun authenticationManager(): AuthenticationManager {
        return ProviderManager(listOf(jaasAuthenticationProvider))
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/vacancies").hasAuthority("VACANCY_VIEW_PUBLIC")
                    .requestMatchers("/api/vacancies/{id}").hasAuthority("VACANCY_VIEW_PUBLIC")
                    .requestMatchers("/api/vacancies/admin/**").hasAuthority("VACANCY_VIEW_ALL")
                    .requestMatchers(HttpMethod.POST, "/api/vacancies").hasAuthority("VACANCY_CREATE_EDIT")
                    .requestMatchers("/api/vacancies/{id}/prepare-publish").hasAuthority("VACANCY_PUBLISH")
                    .requestMatchers("/api/vacancies/{id}/process-payment").hasAuthority("PAYMENT_PROCESS")
                    .requestMatchers("/api/payments/**").hasAuthority("PAYMENT_PROCESS")
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}