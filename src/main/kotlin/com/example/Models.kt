package com.example

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable data class Habit(
    val id: Int = 0, val userId: Int, val name: String,
    val description: String = "", val icon: String = "OK",
    val color: String = "#6366F1", val archived: Boolean = false
)
@Serializable data class NewHabit(
    val name: String, val description: String = "",
    val icon: String = "OK", val color: String = "#6366F1"
)
@Serializable data class User(val id: Int = 0, val username: String, val role: String = "user")
@Serializable data class RegisterRequest(val username: String, val password: String, val role: String = "user")
@Serializable data class LoginRequest(val username: String, val password: String)
@Serializable data class TokenResponse(val token: String, val user: User)
@Serializable data class ErrorResponse(val error: String, val code: Int)
@Serializable data class HabitStats(
    val habitId: Int, val currentStreak: Int, val totalCheckIns: Int,
    val last30Days: List<Boolean>
)

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20).default("user")
    override val primaryKey = PrimaryKey(id)
}

object HabitsTable : Table("habits") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val name = varchar("name", 200)
    val description = text("description").default("")
    val icon = varchar("icon", 16).default("OK")
    val color = varchar("color", 16).default("#6366F1")
    val archived = bool("archived").default(false)
    override val primaryKey = PrimaryKey(id)
}

object CheckInsTable : Table("check_ins") {
    val id = integer("id").autoIncrement()
    val habitId = integer("habit_id").references(HabitsTable.id)
    val userId = integer("user_id").references(UsersTable.id)
    val date = varchar("date", 10)
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(habitId, date) }
}