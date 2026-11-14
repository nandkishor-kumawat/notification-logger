package `in`.i8n.notificationlogger.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.data.model.AppSummary
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.ui.applist.AppList
import `in`.i8n.notificationlogger.ui.notifications.NotificationListScreen
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowsingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyAppListShowsEmptyState() {
        compose.setContent { AppList(emptyList(), Selection.None, {}) }
        compose.onNodeWithText("There are not any notifications yet").assertIsDisplayed()
    }

    @Test
    fun appListShowsAllAppsEntryAndReportsSelection() {
        var selected: Selection? = null
        val apps = listOf(AppSummary("com.example.unknown.app", System.currentTimeMillis(), 3))
        compose.setContent { AppList(apps, Selection.None, { selected = it }) }
        compose.onNodeWithText("All apps").assertIsDisplayed()
        // Unresolvable packages fall back to the package name.
        compose.onNodeWithText("com.example.unknown.app").performClick()
        assertEquals(Selection.App("com.example.unknown.app"), selected)
        compose.onNodeWithText("All apps").performClick()
        assertEquals(Selection.AllApps, selected)
    }

    @Test
    fun notificationListShowsTitleTextAndAppName() {
        val data = listOf(
            NotificationItem(1, "com.example.unknown.app", 1000L, "Hello", "World"),
            NotificationItem(2, "com.example.unknown.app", 2000L, "Download", null, 30, 100),
        )
        compose.setContent {
            val items = flowOf(PagingData.from(data)).collectAsLazyPagingItems()
            NotificationListScreen("All apps", items, showAppName = true, onBack = null)
        }
        compose.onNodeWithText("Hello").assertIsDisplayed()
        compose.onNodeWithText("World").assertIsDisplayed()
        compose.onNodeWithText("Download").assertIsDisplayed()
    }

    @Test
    fun emptyNotificationListShowsEmptyState() {
        compose.setContent {
            val items = flowOf(PagingData.empty<NotificationItem>()).collectAsLazyPagingItems()
            NotificationListScreen("App", items, showAppName = false, onBack = {})
        }
        compose.onNodeWithText("No notifications").assertIsDisplayed()
    }
}
