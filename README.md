\# ✅ Habit Tracker API



REST API для трекера привычек на \*\*Ktor\*\* с JWT-авторизацией, ролями, Swagger UI, PostgreSQL и полным CI/CD через GitHub Actions.



\*\*Живой деплой:\*\*

\- 🌐 API: http://136.234.4.90:8080

\- 📘 Swagger UI: http://136.234.4.90:8080/swagger

\- 📄 OpenAPI JSON: http://136.234.4.90:8080/swagger/openapi.json

\- 🐙 GitHub: https://github.com/ilmifgit/habit-tracker



\## 🛠 Стек



\- \*\*Kotlin 1.9\*\* + \*\*Ktor 2.3.7\*\* (Netty)

\- \*\*kotlinx.serialization\*\* — JSON

\- \*\*Exposed\*\* + \*\*HikariCP\*\* + \*\*PostgreSQL 16\*\*

\- \*\*JWT\*\* (auth0) + роли admin/user

\- \*\*Swagger UI\*\* + OpenAPI 3.0

\- \*\*Gradle 8.5\*\* + \*\*JUnit 5\*\*

\- \*\*Docker\*\* + \*\*docker-compose\*\*

\- \*\*GitHub Actions\*\* — CI/CD



\## 🚀 Быстрый старт (локально)



\### Требования

\- JDK 17

\- Docker Desktop



\### Запуск



```bash

git clone https://github.com/ilmifgit/habit-tracker.git

cd habit-tracker



docker compose up -d

./gradlew run

