package id.lesin.app.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

class IosDatabaseDriverFactory : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = LesinDatabase.Schema,
            name = DATABASE_NAME,
            onConfiguration = { configuration ->
                configuration.copy(
                    extendedConfig = configuration.extendedConfig.copy(foreignKeyConstraints = true),
                )
            },
        )
    }

    private companion object {
        const val DATABASE_NAME = "lesin.db"
    }
}
