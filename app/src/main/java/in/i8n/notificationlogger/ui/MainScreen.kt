package `in`.i8n.notificationlogger.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import `in`.i8n.notificationlogger.capture.PendingIntentHolder
import `in`.i8n.notificationlogger.data.model.NotificationItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import `in`.i8n.notificationlogger.ui.appinfo.AppEntry
import `in`.i8n.notificationlogger.ui.appinfo.InstalledApps
import `in`.i8n.notificationlogger.ui.settings.AppListMenu
import `in`.i8n.notificationlogger.ui.settings.FilterSettingsScreen
import `in`.i8n.notificationlogger.ui.settings.LoggingDisabledCard
import `in`.i8n.notificationlogger.ui.settings.PerAppLogging
import `in`.i8n.notificationlogger.ui.settings.NotificationListMenu

private val TWO_PANE_MIN_WIDTH = 900.dp

private fun copyToClipboard(context: Context, item: NotificationItem) {
    val text = listOfNotNull(item.title, item.text).joinToString("\n")
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
    // Android 13+ shows its own "copied" confirmation.
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionCard: @Composable () -> Unit,
    viewModel: MainViewModel = viewModel(),
) {
    val apps by viewModel.apps.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val context = LocalContext.current
    val items = remember(selection) { viewModel.notificationsFor(selection) }.collectAsLazyPagingItems()
    var showFilter by rememberSaveable { mutableStateOf(false) }

    // Export: remember what to export while the system file picker is open.
    var pendingExport by remember { mutableStateOf<Selection?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val target = pendingExport
        pendingExport = null
        if (uri != null && target != null) viewModel.export(uri, target)
    }
    fun startExport(target: Selection, fileName: String) {
        pendingExport = target
        exportLauncher.launch(fileName)
    }

    if (showFilter) {
        BackHandler { showFilter = false }
        val loggedPackages = apps.orEmpty().map { it.packageName }
        val installed by produceState<List<AppEntry>?>(null, loggedPackages) {
            value = InstalledApps.load(context, loggedPackages)
        }
        FilterSettingsScreen(
            whitelistMode = settings.whitelistMode,
            selectedApps = settings.invertedApps,
            apps = installed,
            onWhitelistMode = viewModel::setWhitelistMode,
            onToggleApp = viewModel::setAppSelected,
            onBack = { showFilter = false },
        )
        return
    }


    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH
        val selectedPackage = (selection as? Selection.App)?.packageName
        val loggingEnabled = selectedPackage == null || settings.shouldLog(selectedPackage)
        val appLabel = selectedPackage?.let { rememberAppInfo(it).value?.label ?: it }
        val detailTitle = when (selection) {
            Selection.None -> ""
            Selection.AllApps -> stringResource(R.string.all_apps)
            is Selection.App -> appLabel.orEmpty()
        }

        val list: @Composable (Modifier) -> Unit = { modifier ->
            Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            AppListMenu(
                                settings.appSorting,
                                viewModel::setAppSorting,
                                onFilterSettings = { showFilter = true },
                                keepingDays = settings.keepingDays,
                                onKeepingDays = viewModel::setKeepingDays,
                                onExportAll = { startExport(Selection.AllApps, "notifications.txt") },
                                onClearAll = { viewModel.clearHistory(Selection.AllApps) },
                                openNotifications = settings.openNotifications,
                                onOpenNotifications = viewModel::setOpenNotifications,
                            )
                        },
                    )
                },
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
                onCopy = { copyToClipboard(context, it) },
                onOpen = if (settings.openNotifications) {
                    { item ->
                        if (!PendingIntentHolder.open(context, item.id)) {
                            Toast.makeText(context, R.string.action_not_available, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    null
                },
                menu = {
                    NotificationListMenu(
                        settings,
                        viewModel::setNotificationSorting,
                        viewModel::setVersionHandling,
                        viewModel::setHideDuplicates,
                        perApp = selectedPackage?.let { pkg ->
                            PerAppLogging(detailTitle, loggingEnabled) { viewModel.setLogging(pkg, it) }
                        },
                        clearTitle = if (selectedPackage != null) {
                            stringResource(R.string.clear_app_title, detailTitle)
                        } else {
                            stringResource(R.string.clear_all_title)
                        },
                        onExport = {
                            val name = if (selectedPackage != null) "notifications-$selectedPackage.txt" else "notifications.txt"
                            startExport(selection, name)
                        },
                        onClear = { viewModel.clearHistory(selection) },
                    )
                },
                banner = if (selectedPackage != null && !loggingEnabled) {
                    { LoggingDisabledCard(detailTitle) }
                } else {
                    null
                },
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
