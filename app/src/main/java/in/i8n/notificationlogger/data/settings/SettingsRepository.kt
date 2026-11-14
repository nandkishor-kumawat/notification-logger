package `in`.i8n.notificationlogger.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.i8n.notificationlogger.data.model.AppSorting
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            whitelistMode = p[WHITELIST_MODE] ?: d.whitelistMode,
            invertedApps = p[INVERTED_APPS] ?: d.invertedApps,
            appSorting = enumOf(p[APP_SORTING], d.appSorting),
            notificationSorting = enumOf(p[NOTIFICATION_SORTING], d.notificationSorting),
            versionHandling = enumOf(p[VERSION_HANDLING], d.versionHandling),
            openNotifications = p[OPEN_NOTIFICATIONS] ?: d.openNotifications,
            keepingDays = p[KEEPING_DAYS] ?: d.keepingDays,
            hideDuplicates = p[HIDE_DUPLICATES] ?: d.hideDuplicates,
            requireAuth = p[REQUIRE_AUTH] ?: d.requireAuth,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun shouldLog(packageName: String): Boolean = current().shouldLog(packageName)

    suspend fun setWhitelistMode(value: Boolean) = edit { it[WHITELIST_MODE] = value }
    suspend fun setInvertedApps(value: Set<String>) = edit { it[INVERTED_APPS] = value }
    suspend fun setAppSorting(value: AppSorting) = edit { it[APP_SORTING] = value.name }
    suspend fun setNotificationSorting(value: NotificationSorting) =
        edit { it[NOTIFICATION_SORTING] = value.name }
    suspend fun setVersionHandling(value: VersionHandling) =
        edit { it[VERSION_HANDLING] = value.name }
    suspend fun setOpenNotifications(value: Boolean) = edit { it[OPEN_NOTIFICATIONS] = value }
    suspend fun setKeepingDays(value: Int) = edit { it[KEEPING_DAYS] = value }
    suspend fun setHideDuplicates(value: Boolean) = edit { it[HIDE_DUPLICATES] = value }
    suspend fun setRequireAuth(value: Boolean) = edit { it[REQUIRE_AUTH] = value }

    /** Turns logging on/off for one app, whatever the current mode. */
    suspend fun setLogging(packageName: String, enabled: Boolean) {
        store.edit { p ->
            val whitelist = p[WHITELIST_MODE] ?: false
            val set = (p[INVERTED_APPS] ?: emptySet()).toMutableSet()
            // shouldLog == (inverted == whitelist), so inverted must equal (enabled == whitelist).
            if (enabled == whitelist) set += packageName else set -= packageName
            p[INVERTED_APPS] = set
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        val WHITELIST_MODE = booleanPreferencesKey("whitelistMode")
        val INVERTED_APPS = stringSetPreferencesKey("invertedApps")
        val APP_SORTING = stringPreferencesKey("appSorting")
        val NOTIFICATION_SORTING = stringPreferencesKey("notificationSorting")
        val VERSION_HANDLING = stringPreferencesKey("versionHandling")
        val OPEN_NOTIFICATIONS = booleanPreferencesKey("openNotifications")
        val KEEPING_DAYS = intPreferencesKey("keepingDays")
        val HIDE_DUPLICATES = booleanPreferencesKey("hideDuplicates")
        val REQUIRE_AUTH = booleanPreferencesKey("requireAuth")
    }
}
