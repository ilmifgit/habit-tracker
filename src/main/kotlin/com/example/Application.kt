package com.example

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module(withSwagger: Boolean = true) {
    Database.init()

    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true })
    }

    install(CallLogging) { level = Level.INFO }

    install(StatusPages) {
        exception<IllegalArgumentException> { c, e ->
            c.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Bad request", 400))
        }
        exception<NoSuchElementException> { c, e ->
            c.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Not found", 404))
        }
        exception<IllegalAccessException> { c, e ->
            c.respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Forbidden", 403))
        }
        exception<Throwable> { c, e ->
            c.application.log.error("Unhandled", e)
            c.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal error", 500))
        }
        status(HttpStatusCode.NotFound) { c, _ ->
            c.respond(HttpStatusCode.NotFound, ErrorResponse("Route not found", 404))
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = Auth.issuer
            verifier(Auth.verifier)
            validate { credential ->
                if (credential.payload.getClaim("username").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Unauthorized", 401))
            }
        }
    }

    routing {
        if (withSwagger) {
            openAPI(path = "openapi", swaggerFile = "openapi.json")
            swaggerUI(path = "swagger", swaggerFile = "openapi.json")
        }
        routes()
    }
}