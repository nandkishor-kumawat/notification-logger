package `in`.i8n.notificationlogger.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.ui.onboarding.PermissionCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionCardTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allowButtonInvokesCallback() {
        var allowed = 0
        compose.setContent { PermissionCard(false, onAllow = { allowed++ }, onOpenAppInfo = {}) }
        compose.onNodeWithText("Notification access required").assertIsDisplayed()
        compose.onNodeWithText("Allow access").performClick()
        assertEquals(1, allowed)
        compose.onNodeWithText("Open App info").assertDoesNotExist()
    }

    @Test
    fun restrictedHintShowsAppInfoButton() {
        var opened = 0
        compose.setContent { PermissionCard(true, onAllow = {}, onOpenAppInfo = { opened++ }) }
        compose.onNodeWithText("Open App info").performClick()
        assertEquals(1, opened)
    }
}
