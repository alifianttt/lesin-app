package id.lesin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.lesin.app.core.database.AndroidDatabaseDriverFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                databaseDriverFactory = AndroidDatabaseDriverFactory(
                    context = applicationContext,
                ),
            )
        }
    }
}
