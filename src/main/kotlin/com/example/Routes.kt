package com.example

import com.example.Auth.hash
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

private fun ResultRow.toHabit() = Habit(
    id = this[HabitsTable.id],
    userId = this[HabitsTable.userId],
    name = this[HabitsTable.name],
    description = this[HabitsTable.description],
    icon = this[HabitsTable.icon],
    color = this[HabitsTable.color],
    archived = this[HabitsTable.archived]
)

fun Route.routes() {

    route("/auth") {

        post("/register") {
            val req = call.receive<RegisterRequest>()
            if (req.username.isBlank() || req.password.length < 3)
                throw IllegalArgumentException("username or password invalid")

            val exists = transaction {
                UsersTable.selectAll().where { UsersTable.username eq req.username }.any()
            }
            if (exists) return@post call.respond(HttpStatusCode.Conflict, ErrorResponse("User exists", 409))

            val user = transaction {
                val id = UsersTable.insert {
                    it[username] = req.username
                    it[passwordHash] = hash(req.password)
                    it[role] = req.role
                } get UsersTable.id
                User(id, req.username, req.role)
            }
            call.respond(HttpStatusCode.Created, user)
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            val row = transaction {
                UsersTable.selectAll().where { UsersTable.username eq req.username }.singleOrNull()
            } ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Bad credentials", 401))

            if (row[UsersTable.passwordHash] != hash(req.password))
                return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Bad credentials", 401))

            val token = Auth.generate(row[UsersTable.id], row[UsersTable.username], row[UsersTable.role])
            call.respond(TokenResponse(token, User(row[UsersTable.id], row[UsersTable.username], row[UsersTable.role])))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val uid = call.userId()
                val u = transaction { UsersTable.selectAll().where { UsersTable.id eq uid }.single() }
                call.respond(User(u[UsersTable.id], u[UsersTable.username], u[UsersTable.role]))
            }
        }
    }

    authenticate("auth-jwt") {
        route("/habits") {

            get {
                val uid = call.userId()
                val search = call.request.queryParameters["search"]
                val list = transaction {
                    var q = HabitsTable.selectAll().where {
                        (HabitsTable.userId eq uid) and (HabitsTable.archived eq false)
                    }
                    if (!search.isNullOrBlank())
                        q = q.andWhere { HabitsTable.name like "%$search%" }
                    q.map { it.toHabit() }
                }
                call.respond(list)
            }

            post {
                val uid = call.userId()
                val b = call.receive<NewHabit>()
                if (b.name.isBlank()) throw IllegalArgumentException("name required")

                val h = transaction {
                    val id = HabitsTable.insert {
                        it[userId] = uid
                        it[name] = b.name
                        it[description] = b.description
                        it[icon] = b.icon
                        it[color] = b.color
                    } get HabitsTable.id
                    HabitsTable.selectAll().where { HabitsTable.id eq id }.single().toHabit()
                }
                call.respond(HttpStatusCode.Created, h)
            }

            get("/{id}") {
                val uid = call.userId()
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("id must be integer")
                val h = transaction {
                    HabitsTable.selectAll().where { HabitsTable.id eq id }.singleOrNull()?.toHabit()
                } ?: throw NoSuchElementException("Habit $id not found")
                if (h.userId != uid && call.userRole() != "admin")
                    throw IllegalAccessException("Forbidden")
                call.respond(h)
            }

            delete("/{id}") {
                val uid = call.userId()
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("id must be integer")
                val owner = transaction {
                    HabitsTable.select(HabitsTable.userId).where { HabitsTable.id eq id }
                        .singleOrNull()?.get(HabitsTable.userId)
                } ?: throw NoSuchElementException("Habit $id not found")
                if (owner != uid && call.userRole() != "admin")
                    throw IllegalAccessException("Forbidden")

                transaction {
                    CheckInsTable.deleteWhere { CheckInsTable.habitId eq id }
                    HabitsTable.deleteWhere { HabitsTable.id eq id }
                }
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/check-in") {
                val uid = call.userId()
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("id must be integer")
                val today = LocalDate.now().toString()

                val habit = transaction {
                    HabitsTable.selectAll().where { HabitsTable.id eq id }.singleOrNull()?.toHabit()
                } ?: throw NoSuchElementException("Habit $id not found")
                if (habit.userId != uid) throw IllegalAccessException("Forbidden")

                val ok = transaction {
                    if (CheckInsTable.selectAll().where {
                            (CheckInsTable.habitId eq id) and (CheckInsTable.date eq today)
                        }.any()) false
                    else {
                        CheckInsTable.insert {
                            it[habitId] = id
                            it[userId] = uid
                            it[date] = today
                        }
                        true
                    }
                }
                if (!ok) return@post call.respond(HttpStatusCode.Conflict, ErrorResponse("Already checked", 409))
                call.respond(HttpStatusCode.Created, mapOf("ok" to true))
            }

            get("/{id}/stats") {
                val uid = call.userId()
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("id must be integer")

                val habit = transaction {
                    HabitsTable.selectAll().where { HabitsTable.id eq id }.singleOrNull()?.toHabit()
                } ?: throw NoSuchElementException("Habit $id not found")
                if (habit.userId != uid) throw IllegalAccessException("Forbidden")

                val dates = transaction {
                    CheckInsTable.select(CheckInsTable.date)
                        .where { CheckInsTable.habitId eq id }
                        .map { LocalDate.parse(it[CheckInsTable.date]) }.toSet()
                }

                var streak = 0
                var d = LocalDate.now()
                while (dates.contains(d)) { streak++; d = d.minusDays(1) }

                val last30 = (29 downTo 0).map {
                    dates.contains(LocalDate.now().minusDays(it.toLong()))
                }

                call.respond(HabitStats(id, streak, dates.size, last30))
            }
        }
    }
}