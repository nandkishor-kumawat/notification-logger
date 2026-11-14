package `in`.i8n.notificationlogger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.i8n.notificationlogger.R
import `in`.i8n.notificationlogger.data.model.AppSorting
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import `in`.i8n.notificationlogger.data.settings.AppSettings

/** Quick "log this app" toggle for the open app. */
data class PerAppLogging(val appLabel: String, val enabled: Boolean, val onChange: (Boolean) -> Unit)

private enum class Dialog { APP_SORT, NOTIFICATION_SORT, VERSIONS, CLEAR, KEEP, OPEN_DISCLAIMER }

@Composable
fun AppListMenu(
    sorting: AppSorting,
    onSorting: (AppSorting) -> Unit,
    onFilterSettings: () -> Unit = {},
    keepingDays: Int = AppSettings.KEEP_FOREVER,
    onKeepingDays: (Int) -> Unit = {},
    onExportAll: () -> Unit = {},
    onClearAll: () -> Unit = {},
    openNotifications: Boolean = false,
    onOpenNotifications: (Boolean) -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    OverflowButton(menuOpen, { menuOpen = it }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_sort_apps)) },
            onClick = { menuOpen = false; dialog = Dialog.APP_SORT },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.filter_settings)) },
            onClick = { menuOpen = false; onFilterSettings() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_open_notifications)) },
            trailingIcon = { Checkbox(openNotifications, onCheckedChange = null) },
            onClick = {
                menuOpen = false
                // Turning it on needs the user to accept the disclaimer first.
                if (openNotifications) onOpenNotifications(false) else dialog = Dialog.OPEN_DISCLAIMER
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_export)) },
            onClick = { menuOpen = false; onExportAll() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_auto_delete)) },
            onClick = { menuOpen = false; dialog = Dialog.KEEP },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_clear_history)) },
            onClick = { menuOpen = false; dialog = Dialog.CLEAR },
        )
    }
    when (dialog) {
        Dialog.OPEN_DISCLAIMER -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.open_disclaimer_title)) },
            text = { Text(stringResource(R.string.open_disclaimer_body)) },
            confirmButton = {
                TextButton(onClick = { onOpenNotifications(true); dialog = null }) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.reject)) } },
        )
        Dialog.CLEAR -> ConfirmClearDialog(
            title = stringResource(R.string.clear_all_title),
            onConfirm = onClearAll,
            onDismiss = { dialog = null },
        )
        Dialog.KEEP -> ChoiceDialog(
            title = stringResource(R.string.auto_delete_title),
            options = listOf(AppSettings.KEEP_FOREVER to stringResource(R.string.keep_never)) +
                listOf(1, 7, 30, 90, 365).map { it to pluralStringResource(R.plurals.keep_days, it, it) },
            selected = keepingDays,
            onSelect = onKeepingDays,
            onDismiss = { dialog = null },
        )
        else -> Unit
    }
    if (dialog == Dialog.APP_SORT) {
        ChoiceDialog(
            title = stringResource(R.string.menu_sort_apps),
            options = listOf(
                AppSorting.NAME to stringResource(R.string.sort_alphabetical),
                AppSorting.NEWEST_FIRST to stringResource(R.string.sort_newest_first),
                AppSorting.OLDEST_FIRST to stringResource(R.string.sort_oldest_first),
            ),
            selected = sorting,
            onSelect = onSorting,
            onDismiss = { dialog = null },
        )
    }
}

@Composable
fun NotificationListMenu(
    settings: AppSettings,
    onSorting: (NotificationSorting) -> Unit,
    onVersions: (VersionHandling) -> Unit,
    onHideDuplicates: (Boolean) -> Unit,
    perApp: PerAppLogging? = null,
    clearTitle: String = "",
    onExport: () -> Unit = {},
    onClear: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    OverflowButton(menuOpen, { menuOpen = it }) {
        if (perApp != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_log_app, perApp.appLabel)) },
                trailingIcon = { Checkbox(perApp.enabled, onCheckedChange = null) },
                onClick = { perApp.onChange(!perApp.enabled) },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_sort_notifications)) },
            onClick = { menuOpen = false; dialog = Dialog.NOTIFICATION_SORT },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_notification_updates)) },
            onClick = { menuOpen = false; dialog = Dialog.VERSIONS },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_hide_duplicates)) },
            trailingIcon = { Checkbox(settings.hideDuplicates, onCheckedChange = null) },
            onClick = { onHideDuplicates(!settings.hideDuplicates) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_export)) },
            onClick = { menuOpen = false; onExport() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_clear_history)) },
            onClick = { menuOpen = false; dialog = Dialog.CLEAR },
        )
    }
    when (dialog) {
        Dialog.NOTIFICATION_SORT -> ChoiceDialog(
            title = stringResource(R.string.menu_sort_notifications),
            options = listOf(
                NotificationSorting.NEWEST_FIRST to stringResource(R.string.sort_newest_first),
                NotificationSorting.OLDEST_FIRST to stringResource(R.string.sort_oldest_first),
            ),
            selected = settings.notificationSorting,
            onSelect = onSorting,
            onDismiss = { dialog = null },
        )
        Dialog.CLEAR -> ConfirmClearDialog(
            title = clearTitle,
            onConfirm = onClear,
            onDismiss = { dialog = null },
        )
        Dialog.VERSIONS -> ChoiceDialog(
            title = stringResource(R.string.menu_notification_updates),
            options = listOf(
                VersionHandling.SHOW_ALL to stringResource(R.string.versions_show_all),
                VersionHandling.OLDEST_ONLY to stringResource(R.string.versions_oldest_only),
                VersionHandling.NEWEST_ONLY to stringResource(R.string.versions_newest_only),
            ),
            selected = settings.versionHandling,
            onSelect = onVersions,
            onDismiss = { dialog = null },
        )
        else -> Unit
    }
}

@Composable
private fun OverflowButton(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    items: @Composable () -> Unit,
) {
    IconButton(onClick = { onOpenChange(true) }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
    }
    DropdownMenu(expanded = open, onDismissRequest = { onOpenChange(false) }) { items() }
}

/** Single-choice dialog; picking an option applies it and closes the dialog. */
@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Text(label, Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ConfirmClearDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(stringResource(R.string.clear_message)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(stringResource(R.string.clear_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
