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
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import `in`.i8n.notificationlogger.ui.appinfo.AppInfoProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val settings = SettingsRepository(application).settings

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

    private data class ListConfig(
        val selection: Selection,
        val sorting: `in`.i8n.notificationlogger.data.model.NotificationSorting,
        val versions: `in`.i8n.notificationlogger.data.model.VersionHandling,
        val hideDuplicates: Boolean,
    )

    val notifications: Flow<PagingData<NotificationItem>> = combine(selection, settings) { sel, s ->
        ListConfig(sel, s.notificationSorting, s.versionHandling, s.hideDuplicates)
    }
        .distinctUntilChanged()
        .flatMapLatest { c ->
            val pkg = when (val sel = c.selection) {
                Selection.None -> return@flatMapLatest emptyFlow()
                Selection.AllApps -> null
                is Selection.App -> sel.packageName
            }
            Pager(PagingConfig(pageSize = 40, enablePlaceholders = false)) {
                dao.paged(NotificationQuery.build(pkg, c.sorting, c.versions, c.hideDuplicates))
            }.flow
        }
        .cachedIn(viewModelScope)

    fun select(value: Selection) {
        selection.value = value
    }
}
