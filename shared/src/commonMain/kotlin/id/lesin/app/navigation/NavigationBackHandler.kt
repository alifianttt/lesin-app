package id.lesin.app.navigation

import androidx.compose.runtime.Composable

@Composable
internal expect fun NavigationBackHandler(enabled: Boolean, onBack: () -> Unit)
