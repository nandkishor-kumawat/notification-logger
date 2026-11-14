package `in`.i8n.notificationlogger.ui.applist

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.i8n.notificationlogger.R
import `in`.i8n.notificationlogger.data.model.AppSummary
import `in`.i8n.notificationlogger.ui.MainViewModel
import `in`.i8n.notificationlogger.ui.Selection
import `in`.i8n.notificationlogger.ui.appinfo.rememberAppInfo

/** [apps] null = still loading. Emits nothing special above the list; the caller adds cards. */
@Composable
fun AppList(
    apps: List<AppSummary>?,
    selection: Selection,
    onSelect: (Selection) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    LazyColumn(modifier.fillMaxSize()) {
        item { Box(Modifier.padding(horizontal = 16.dp)) { header() } }
        when {
            apps == null -> Unit
            apps.isEmpty() -> item {
                Text(
                    stringResource(R.string.app_list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                )
            }
            else -> {
                item(key = "all") {
                    val total = apps.sumOf { it.count }
                    val latest = apps.maxOf { it.lastTime }
                    AppRow(
                        label = stringResource(R.string.all_apps),
                        packageName = null,
                        summary = AppSummary("", latest, total),
                        selected = selection == Selection.AllApps,
                        onClick = { onSelect(Selection.AllApps) },
                    )
                }
                items(apps, key = { it.packageName }) { app ->
                    AppRow(
                        label = null,
                        packageName = app.packageName,
                        summary = app,
                        selected = selection == Selection.App(app.packageName),
                        onClick = { onSelect(Selection.App(app.packageName)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    label: String?,
    packageName: String?,
    summary: AppSummary,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val info = packageName?.let { rememberAppInfo(it).value }
    val title = label ?: info?.label ?: packageName.orEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconModifier = Modifier.size(40.dp).clip(CircleShape)
        if (info?.icon != null) {
            Image(info.icon, contentDescription = null, modifier = iconModifier)
        } else {
            Box(iconModifier.background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.notification_count, summary.count, summary.count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            DateUtils.getRelativeTimeSpanString(
                summary.lastTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
            ).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
