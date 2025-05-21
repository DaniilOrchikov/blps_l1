package com.example.blps.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenUtil {
    @Value("\${jwt.secret:defaultSecretKeyThatIsAtLeast256BitsLongForHS512Signature}")
    private lateinit var secretString: String

    @Value("\${jwt.expiration:3600000}") // 1 час по умолчанию
    private var validityInMilliseconds: Long = 0

    private fun getSigningKey(): SecretKey {
        val keyBytes = secretString.toByteArray()
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(username: String, authorities: Collection<String>): String {
        val now = Date()
        val validity = Date(now.time + validityInMilliseconds)

        return Jwts.builder()
            .setSubject(username)
            .claim("authorities", authorities)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(getSigningKey())
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            !isTokenExpired(token)
        } catch (e: Exception) {
            false
        }
    }

    fun getUsernameFromToken(token: String): String {
        return getClaimFromToken(token).subject
    }

    @Suppress("UNCHECKED_CAST")
    fun getAuthoritiesFromToken(token: String): List<String> {
        val claims = getClaimFromToken(token)
        return claims.get("authorities", List::class.java) as? List<String> ?: emptyList()
    }

    private fun isTokenExpired(token: String): Boolean {
        val expiration = getClaimFromToken(token).expiration
        return expiration.before(Date())
    }

    private fun getClaimFromToken(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body
    }
}