package `in`.i8n.notificationlogger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.i8n.notificationlogger.R
import `in`.i8n.notificationlogger.ui.appinfo.rememberAppInfo
import `in`.i8n.notificationlogger.ui.applist.AppList
import `in`.i8n.notificationlogger.ui.notifications.NotificationListScreen

private val TWO_PANE_MIN_WIDTH = 900.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionCard: @Composable () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val apps by viewModel.apps.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val items = viewModel.notifications.collectAsLazyPagingItems()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH
        val selectedPackage = (selection as? Selection.App)?.packageName
        val appLabel = selectedPackage?.let { rememberAppInfo(it).value?.label ?: it }
        val detailTitle = when (selection) {
            Selection.None -> ""
            Selection.AllApps -> stringResource(R.string.all_apps)
            is Selection.App -> appLabel.orEmpty()
        }

        val list: @Composable (Modifier) -> Unit = { modifier ->
            Scaffold(
                modifier = modifier,
                topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    AppList(apps, selection, viewModel::select, header = permissionCard)
                }
            }
        }
        val detail: @Composable (Modifier, Boolean) -> Unit = { modifier, showBack ->
            NotificationListScreen(
                title = detailTitle,
                items = items,
                showAppName = selection == Selection.AllApps,
                onBack = if (showBack) ({ viewModel.select(Selection.None) }) else null,
                modifier = modifier,
            )
        }

        if (twoPane) {
            Row(Modifier.fillMaxSize()) {
                list(Modifier.width(360.dp).fillMaxHeight())
                HorizontalDivider(Modifier.fillMaxHeight().width(1.dp))
                if (selection == Selection.None) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.select_app_hint), style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    detail(Modifier.weight(1f).fillMaxHeight(), false)
                }
            }
        } else if (selection == Selection.None) {
            list(Modifier.fillMaxSize())
        } else {
            BackHandler { viewModel.select(Selection.None) }
            detail(Modifier.fillMaxSize(), true)
        }
    }
}
