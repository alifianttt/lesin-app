package id.lesin.app.core.database

import app.cash.sqldelight.db.SqlDriver

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
