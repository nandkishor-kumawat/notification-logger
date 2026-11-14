package `in`.i8n.notificationlogger.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.db.NotificationQuery
import `in`.i8n.notificationlogger.data.model.AppSorting
import `in`.i8n.notificationlogger.data.model.AppSummary
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import `in`.i8n.notificationlogger.data.settings.AppSettings
import kotlinx.coroutines.launch
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import `in`.i8n.notificationlogger.ui.appinfo.AppInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the user is looking at. */
sealed interface Selection {
    data object None : Selection
    data object AllApps : Selection
    data class App(val packageName: String) : Selection
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.get(application).notificationDao()
    private val settingsRepo = SettingsRepository(application)
    private val settings = settingsRepo.settings

    val appSettings: StateFlow<AppSettings> =
        settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setAppSorting(v: AppSorting) = viewModelScope.launch { settingsRepo.setAppSorting(v) }
    fun setNotificationSorting(v: NotificationSorting) =
        viewModelScope.launch { settingsRepo.setNotificationSorting(v) }
    fun setVersionHandling(v: VersionHandling) =
        viewModelScope.launch { settingsRepo.setVersionHandling(v) }
    fun setWhitelistMode(v: Boolean) = viewModelScope.launch { settingsRepo.setWhitelistMode(v) }
    /** [selected] = membership in the filter set (excluded / only-logged, depending on mode). */
    fun setAppSelected(pkg: String, selected: Boolean) = viewModelScope.launch {
        val set = settingsRepo.current().invertedApps.toMutableSet()
        if (selected) set += pkg else set -= pkg
        settingsRepo.setInvertedApps(set)
    }
    fun setLogging(pkg: String, enabled: Boolean) = viewModelScope.launch { settingsRepo.setLogging(pkg, enabled) }

    fun setHideDuplicates(v: Boolean) = viewModelScope.launch { settingsRepo.setHideDuplicates(v) }

    val selection = MutableStateFlow<Selection>(Selection.None)

    /** Apps with logged notifications; null until the first load. */
    val apps: StateFlow<List<AppSummary>?> = settings
        .map { it.appSorting }
        .distinctUntilChanged()
        .flatMapLatest { sorting ->
            when (sorting) {
                AppSorting.NEWEST_FIRST -> dao.appsNewestFirst()
                AppSorting.OLDEST_FIRST -> dao.appsOldestFirst()
                AppSorting.NAME -> dao.appsNewestFirst().map { list ->
                    list.sortedBy { AppInfoProvider.label(application, it.packageName).lowercase() }
                }.flowOn(Dispatchers.Default)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private class CachedList(val flow: Flow<PagingData<NotificationItem>>, val scope: CoroutineScope)

    /** Recently opened lists, so going back to an app is instant. Evicted ones stop collecting. */
    private val lists = object : LinkedHashMap<Selection, CachedList>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Selection, CachedList>): Boolean {
            val evict = size > MAX_CACHED_LISTS
            if (evict) eldest.value.scope.cancel()
            return evict
        }
    }

    /**
     * Notifications for [selection]. Every selection has its own flow, so a screen opened for a
     * different app starts empty instead of briefly showing the previous app's rows.
     */
    fun notificationsFor(selection: Selection): Flow<PagingData<NotificationItem>> =
        lists.getOrPut(selection) {
            val scope = CoroutineScope(
                viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]),
            )
            val pkg = when (selection) {
                Selection.None -> null
                Selection.AllApps -> null
                is Selection.App -> selection.packageName
            }
            val flow = if (selection == Selection.None) {
                emptyFlow()
            } else {
                settings
                    .map { Triple(it.notificationSorting, it.versionHandling, it.hideDuplicates) }
                    .distinctUntilChanged()
                    .flatMapLatest { (sorting, versions, hideDuplicates) ->
                        Pager(PagingConfig(pageSize = 40, enablePlaceholders = false)) {
                            dao.paged(NotificationQuery.build(pkg, sorting, versions, hideDuplicates))
                        }.flow
                    }
                    .cachedIn(scope)
            }
            CachedList(flow, scope)
        }.flow

    private companion object {
        const val MAX_CACHED_LISTS = 4
    }

    fun select(value: Selection) {
        selection.value = value
    }
}
