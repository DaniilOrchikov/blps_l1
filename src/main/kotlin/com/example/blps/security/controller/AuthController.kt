package com.example.blps.security.controller

import com.example.blps.security.model.User
import com.example.blps.security.service.XmlUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val xmlUserService: XmlUserService,
    private val passwordEncoder: PasswordEncoder
) {
    @PostMapping("/register")
    fun register(@RequestBody registerRequest: RegisterRequest): ResponseEntity<*> {
        if (xmlUserService.existsByUsername(registerRequest.username)) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Username is already taken!"))
        }

        if (xmlUserService.existsByEmail(registerRequest.email)) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Email is already in use!"))
        }

        val allowedRoles = setOf("CANDIDATE", "HR_MANAGER")
        if (!allowedRoles.contains(registerRequest.role.uppercase())) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Invalid role specified!"))
        }

        val user = User(
            username = registerRequest.username,
            email = registerRequest.email,
            password = passwordEncoder.encode(registerRequest.password),
            roles = mutableListOf(registerRequest.role.uppercase())
        )

        xmlUserService.saveUser(user)

        return ResponseEntity.ok(mapOf("message" to "User registered successfully!"))
    }
}

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: String = "CANDIDATE"
)