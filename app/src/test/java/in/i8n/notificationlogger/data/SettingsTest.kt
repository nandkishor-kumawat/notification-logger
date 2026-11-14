package `in`.i8n.notificationlogger.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import `in`.i8n.notificationlogger.data.model.AppSorting
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import `in`.i8n.notificationlogger.data.settings.AppSettings
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var file: File
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        file = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
        repo = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
    }

    @After
    fun tearDown() {
        scope.cancel()
        file.delete()
    }

    @Test
    fun blacklistModeLogsEverythingExceptInverted() {
        val s = AppSettings(whitelistMode = false, invertedApps = setOf("blocked"))
        assertTrue(s.shouldLog("other"))
        assertFalse(s.shouldLog("blocked"))
    }

    @Test
    fun whitelistModeLogsOnlyInverted() {
        val s = AppSettings(whitelistMode = true, invertedApps = setOf("allowed"))
        assertTrue(s.shouldLog("allowed"))
        assertFalse(s.shouldLog("other"))
    }

    @Test
    fun defaults() = runBlocking {
        assertEquals(AppSettings(), repo.current())
    }

    @Test
    fun roundTripsAllKeys() = runBlocking {
        repo.setWhitelistMode(true)
        repo.setInvertedApps(setOf("x", "y"))
        repo.setAppSorting(AppSorting.NAME)
        repo.setNotificationSorting(NotificationSorting.OLDEST_FIRST)
        repo.setVersionHandling(VersionHandling.NEWEST_ONLY)
        repo.setOpenNotifications(true)
        repo.setKeepingDays(30)
        repo.setHideDuplicates(true)
        repo.setRequireAuth(true)
        assertEquals(
            AppSettings(
                whitelistMode = true,
                invertedApps = setOf("x", "y"),
                appSorting = AppSorting.NAME,
                notificationSorting = NotificationSorting.OLDEST_FIRST,
                versionHandling = VersionHandling.NEWEST_ONLY,
                openNotifications = true,
                keepingDays = 30,
                hideDuplicates = true,
                requireAuth = true,
            ),
            repo.current(),
        )
    }

    @Test
    fun setLoggingWorksInBothModes() = runBlocking {
        repo.setLogging("app", false)
        assertFalse(repo.shouldLog("app"))
        repo.setLogging("app", true)
        assertTrue(repo.shouldLog("app"))

        repo.setWhitelistMode(true)
        repo.setInvertedApps(emptySet())
        assertFalse(repo.shouldLog("app"))
        repo.setLogging("app", true)
        assertTrue(repo.shouldLog("app"))
        repo.setLogging("app", false)
        assertFalse(repo.shouldLog("app"))
    }
}
