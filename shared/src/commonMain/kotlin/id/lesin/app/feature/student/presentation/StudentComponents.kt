package id.lesin.app.feature.student.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import id.lesin.app.core.designsystem.LesinColors
import id.lesin.app.core.designsystem.LesinIcons
import id.lesin.app.core.designsystem.LesinSpacing

@Composable
internal fun StudentHeader(
    title: String,
    onBack: () -> Unit,
    enabled: Boolean = true,
    action: @Composable () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(vertical = LesinSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, enabled = enabled) {
            Icon(LesinIcons.Back, contentDescription = "Back")
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        action()
    }
}

@Composable
internal fun StudentLoading(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(LesinSpacing.Section), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = LesinColors.Ink)
    }
}

@Composable
internal fun StudentMessage(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = LesinSpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(LesinSpacing.Sm),
    ) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Text(message, color = LesinColors.Muted, fontSize = 14.sp)
        if (actionLabel != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
internal fun StudentError(message: String, onRetry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = LesinSpacing.Md)) {
        Text(message, color = LesinColors.Error, fontSize = 14.sp)
        if (onRetry != null) TextButton(onClick = onRetry) { Text("Try again") }
    }
}
