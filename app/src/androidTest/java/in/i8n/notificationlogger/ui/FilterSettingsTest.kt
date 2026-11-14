package `in`.i8n.notificationlogger.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.ui.appinfo.AppEntry
import `in`.i8n.notificationlogger.ui.settings.FilterSettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val apps = listOf(AppEntry("com.x.alpha", "Alpha"), AppEntry("com.x.beta", "Beta"))

    @Test
    fun searchFiltersAndToggleAndModeReport() {
        var toggled: Pair<String, Boolean>? = null
        var mode: Boolean? = null
        compose.setContent {
            FilterSettingsScreen(false, setOf("com.x.beta"), apps, { mode = it }, { p, s -> toggled = p to s }, {})
        }
        compose.onNodeWithText("Alpha").performClick()
        assertEquals("com.x.alpha" to true, toggled)
        compose.onNodeWithText("Beta").performClick() // already selected -> deselect
        assertEquals("com.x.beta" to false, toggled)

        compose.onNodeWithText("Log only the following apps").performClick()
        assertEquals(true, mode)

        compose.onNodeWithText("Search Apps").performTextInput("bet")
        compose.onNodeWithText("Alpha").assertDoesNotExist()
        compose.onNodeWithText("Beta").assertExists()
    }
}
