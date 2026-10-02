package com.example

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTest {

    private fun uniqueName() = "test_${System.currentTimeMillis()}_${(0..99999).random()}"

    private fun ApplicationTestBuilder.setup() {
        environment {
            config = MapApplicationConfig()
        }
        application { module(withSwagger = false) }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) { json() }
    }

    private fun extractToken(body: String): String =
        Json.parseToJsonElement(body).jsonObject["token"]!!.jsonPrimitive.content

    @Test
    fun `404 on unknown route`() = testApplication {
        setup()
        val client = jsonClient()
        assertEquals(HttpStatusCode.NotFound, client.get("/does-not-exist").status)
    }

    @Test
    fun `401 without token`() = testApplication {
        setup()
        val client = jsonClient()
        assertEquals(HttpStatusCode.Unauthorized, client.get("/habits").status)
    }

    @Test
    fun `401 on wrong password`() = testApplication {
        setup()
        val client = jsonClient()
        val r = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"no_such_user_xyz","password":"x"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, r.status)
    }

    @Test
    fun `400 on empty register`() = testApplication {
        setup()
        val client = jsonClient()
        val r = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"","password":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, r.status)
    }

    @Test
    fun `full cycle register login create habit`() = testApplication {
        setup()
        val client = jsonClient()

        val user = uniqueName()
        val pwd = "pwd12345"

        val reg = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$user","password":"$pwd"}""")
        }
        assertEquals(HttpStatusCode.Created, reg.status)

        val login = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$user","password":"$pwd"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val token = extractToken(login.bodyAsText())
        assertTrue(token.isNotEmpty(), "JWT must not be empty")

        val create = client.post("/habits") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Test Habit"}""")
        }
        assertEquals(HttpStatusCode.Created, create.status)

        val list = client.get("/habits") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, list.status)
        assertTrue(list.bodyAsText().contains("Test Habit"))
    }
}