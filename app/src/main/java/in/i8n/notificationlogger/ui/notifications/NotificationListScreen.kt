package `in`.i8n.notificationlogger.ui.notifications

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
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
    banner: (@Composable () -> Unit)? = null,
    onCopy: (NotificationItem) -> Unit = {},
    /** Null = tapping does nothing (the "open on tap" setting is off). */
    onOpen: ((NotificationItem) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    FollowNewestItems(items, listState)
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (banner != null) Box(Modifier.padding(12.dp)) { banner() }
            if (items.itemCount == 0) {
                // While the first page loads, show nothing rather than a misleading empty message.
                if (items.loadState.refresh !is LoadState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.notification_list_empty), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                        items[index]?.let { NotificationCard(it, showAppName, onClick = onOpen?.let { open -> { open(it) } }, onLongClick = { onCopy(it) }) }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NotificationCard(
    item: NotificationItem,
    showAppName: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Card(modifier.fillMaxWidth().combinedClickable(
        enabled = onClick != null || onLongClick != null,
        onClickLabel = stringResource(R.string.a11y_open_notification),
        onClick = { onClick?.invoke() },
        onLongClickLabel = stringResource(R.string.a11y_copy_notification),
        onLongClick = onLongClick,
    )) {
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
                rememberTimeFormat().format(Date(item.time)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "14 Nov, 10:39:05" in the user's locale and 12/24-hour preference. */
@Composable
private fun rememberTimeFormat(): SimpleDateFormat = remember {
    val locale = Locale.getDefault()
    SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "dMMMjmmss"), locale)
}

/**
 * With newest-first sorting a new notification is inserted at the top, and the list keeps the
 * previously visible item in place, so the new one would appear off-screen. If the user was
 * looking at the top, scroll up to show it; if they had scrolled down to read, leave them be.
 */
@Composable
private fun FollowNewestItems(items: LazyPagingItems<NotificationItem>, listState: LazyListState) {
    var previousFirstId by remember { mutableStateOf<Long?>(null) }
    val firstId = if (items.itemCount > 0) items.peek(0)?.id else null
    LaunchedEffect(firstId) {
        val previous = previousFirstId
        previousFirstId = firstId
        if (previous == null || firstId == null || previous == firstId) return@LaunchedEffect
        // How many items were added above the old first item (-1 = it's gone, e.g. re-sorted).
        val added = items.itemSnapshotList.indexOfFirst { it?.id == previous }
        withFrameNanos { } // let the list apply the insertion before reading its position
        if (added > 0 && listState.firstVisibleItemIndex <= added) listState.animateScrollToItem(0)
    }
}
