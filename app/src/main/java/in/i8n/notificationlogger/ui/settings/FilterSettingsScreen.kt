package `in`.i8n.notificationlogger.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.i8n.notificationlogger.R
import `in`.i8n.notificationlogger.ui.appinfo.AppEntry
import `in`.i8n.notificationlogger.ui.appinfo.rememberAppInfo

/**
 * Choose which apps are logged. [selectedApps] is the set that deviates from the default:
 * excluded apps in blacklist mode, the only logged apps in whitelist mode. Switching mode keeps it.
 * [apps] null = still loading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSettingsScreen(
    whitelistMode: Boolean,
    selectedApps: Set<String>,
    apps: List<AppEntry>?,
    onWhitelistMode: (Boolean) -> Unit,
    onToggleApp: (packageName: String, selected: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    // Apps selected when the list loaded come first. The order is fixed at that point so rows
    // don't jump around while the user ticks boxes.
    val initiallySelected = remember(apps == null) { selectedApps }
    val ordered = remember(apps, initiallySelected) {
        apps?.sortedBy { it.packageName !in initiallySelected }
    }
    val shown = ordered?.filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.filter_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ModeRow(stringResource(R.string.mode_blacklist), selected = !whitelistMode) { onWhitelistMode(false) }
            ModeRow(stringResource(R.string.mode_whitelist), selected = whitelistMode) { onWhitelistMode(true) }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (shown == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.packageName }) { app ->
                    val checked = app.packageName in selectedApps
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggleApp(app.packageName, !checked) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val icon = rememberAppInfo(app.packageName).value?.icon
                        val iconModifier = Modifier.size(36.dp).clip(CircleShape)
                        if (icon != null) {
                            Image(icon, null, iconModifier)
                        } else {
                            Box(iconModifier.background(MaterialTheme.colorScheme.surfaceVariant))
                        }
                        Text(
                            app.label,
                            Modifier.weight(1f).padding(horizontal = 16.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Checkbox(checked, onCheckedChange = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp))
    }
}
