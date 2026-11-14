package `in`.i8n.notificationlogger.ui.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import `in`.i8n.notificationlogger.R
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.ui.appinfo.rememberAppInfo

/** One app's (or all apps') notifications. [onBack] null = no back arrow (two-pane layout). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    title: String,
    items: LazyPagingItems<NotificationItem>,
    showAppName: Boolean,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                actions = { menu() },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (items.itemCount == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.notification_list_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                    items[index]?.let { NotificationCard(it, showAppName) }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem, showAppName: Boolean, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showAppName) {
                val label = rememberAppInfo(item.packageName).value?.label ?: item.packageName
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            item.title?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            item.text?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (item.progressIndeterminate) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else if (item.progressMax > 0) {
                LinearProgressIndicator(
                    progress = { (item.progress.toFloat() / item.progressMax).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                DateUtils.formatDateTime(
                    androidx.compose.ui.platform.LocalContext.current,
                    item.time,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
