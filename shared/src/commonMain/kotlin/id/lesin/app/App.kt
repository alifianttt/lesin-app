package id.lesin.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import id.lesin.app.core.database.DatabaseDriverFactory
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinTheme
import id.lesin.app.core.di.appModules
import id.lesin.app.feature.home.presentation.HomeScreen
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App(
    databaseDriverFactory: DatabaseDriverFactory,
) {
    KoinApplication(
        configuration = koinConfiguration {
            modules(appModules(databaseDriverFactory))
        },
    ) {
        LesinTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = LesinColors.Canvas,
            ) {
                HomeScreen()
            }
        }
    }
}
