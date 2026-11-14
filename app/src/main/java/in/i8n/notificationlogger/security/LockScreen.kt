package `in`.i8n.notificationlogger.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.i8n.notificationlogger.R

/** Shown instead of the app while it is locked. Contains no notification data. */
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Scaffold(Modifier.fillMaxSize()) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.auth_prompt_title), style = MaterialTheme.typography.titleMedium)
            Button(onClick = onUnlock) { Text(stringResource(R.string.auth_unlock)) }
        }
    }
}
