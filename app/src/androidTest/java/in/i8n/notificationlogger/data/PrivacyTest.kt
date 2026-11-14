package `in`.i8n.notificationlogger.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun appHasNoInternetPermission() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val perms = info.requestedPermissions.orEmpty().toList()
        assertFalse("INTERNET must not be requested: $perms", perms.contains("android.permission.INTERNET"))
    }

    @Test
    fun backupIsDisabled() {
        val flags = context.applicationInfo.flags
        assertEquals(0, flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
