package com.example

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Database {
    fun init() {
        val url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/habit_tracker"
        val user = System.getenv("DB_USER") ?: "postgres"
        val pass = System.getenv("DB_PASSWORD") ?: "postgres"

        val cfg = HikariConfig().apply {
            jdbcUrl = url
            username = user
            password = pass
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        Database.connect(HikariDataSource(cfg))
        transaction {
            SchemaUtils.createMissingTablesAndColumns(UsersTable, HabitsTable, CheckInsTable)
        }
    }
}