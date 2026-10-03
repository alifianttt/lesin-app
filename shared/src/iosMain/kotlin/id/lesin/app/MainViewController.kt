package id.lesin.app

import androidx.compose.ui.window.ComposeUIViewController
import id.lesin.app.core.database.IosDatabaseDriverFactory

fun MainViewController() = ComposeUIViewController {
    App(
        databaseDriverFactory = IosDatabaseDriverFactory(),
    )
}
