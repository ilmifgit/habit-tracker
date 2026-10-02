package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.Date

object Auth {
    private val SECRET = System.getenv("JWT_SECRET") ?: "dev-secret-change-me-please-32chars-min"
    private const val ISSUER = "habit-tracker"

    val algorithm = Algorithm.HMAC256(SECRET)
    val verifier = JWT.require(algorithm).withIssuer(ISSUER).build()
    val issuer = ISSUER

    fun generate(id: Int, username: String, role: String): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withClaim("id", id)
            .withClaim("username", username)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L))
            .sign(algorithm)

    fun hash(pwd: String) = pwd.hashCode().toString()
}

fun ApplicationCall.userId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("id").asInt()

fun ApplicationCall.userRole(): String =
    principal<JWTPrincipal>()!!.payload.getClaim("role").asString()