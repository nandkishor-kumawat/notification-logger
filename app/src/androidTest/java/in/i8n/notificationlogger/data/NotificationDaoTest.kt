package `in`.i8n.notificationlogger.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.data.db.ActiveNotificationDao
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.db.NotificationDao
import `in`.i8n.notificationlogger.data.db.NotificationQuery
import `in`.i8n.notificationlogger.data.model.ActiveNotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: NotificationDao
    private lateinit var active: ActiveNotificationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.notificationDao()
        active = db.activeNotificationDao()
    }

    @After
    fun tearDown() = db.close()

    private fun item(pkg: String = "a", time: Long, title: String? = "t", text: String? = "x") =
        NotificationItem(packageName = pkg, time = time, title = title, text = text)

    private fun query(
        pkg: String? = null,
        sorting: NotificationSorting = NotificationSorting.NEWEST_FIRST,
        versions: VersionHandling = VersionHandling.SHOW_ALL,
        hideDuplicates: Boolean = false,
    ): List<NotificationItem> = runBlocking {
        val source = dao.paged(NotificationQuery.build(pkg, sorting, versions, hideDuplicates))
        val result = source.load(PagingSource.LoadParams.Refresh(null, 100, false))
        (result as PagingSource.LoadResult.Page).data
    }

    @Test
    fun insertStartsOwnDuplicateGroup() = runBlocking {
        val id = dao.insert(item(time = 1))
        val saved = dao.getById(id)!!
        assertEquals(id, saved.duplicateGroupId)
        assertTrue(saved.isOldestVersion && saved.isNewestVersion)
    }

    @Test
    fun identicalConsecutiveNotificationsShareGroup() = runBlocking {
        val first = dao.insert(item(time = 1))
        val second = dao.insert(item(time = 2))
        val third = dao.insert(item(time = 3))
        assertEquals(first, dao.getById(second)!!.duplicateGroupId)
        assertEquals(first, dao.getById(third)!!.duplicateGroupId)
    }

    @Test
    fun differentTextBreaksGroup_andOtherAppsDoNotInterfere() = runBlocking {
        val first = dao.insert(item(time = 1))
        dao.insert(item(pkg = "b", time = 2, text = "other"))
        val same = dao.insert(item(time = 3)) // last of app "a" is still identical
        val changed = dao.insert(item(time = 4, text = "different"))
        val back = dao.insert(item(time = 5)) // identical to first, but not to the last one
        assertEquals(first, dao.getById(same)!!.duplicateGroupId)
        assertEquals(changed, dao.getById(changed)!!.duplicateGroupId)
        assertEquals(back, dao.getById(back)!!.duplicateGroupId)
    }

    @Test
    fun updateMarksVersions() = runBlocking {
        val v1 = dao.insert(item(time = 1, text = "10%"))
        val v2 = dao.insert(item(time = 2, text = "50%"), previousVersionId = v1)
        val v3 = dao.insert(item(time = 3, text = "100%"), previousVersionId = v2)
        val all = listOf(v1, v2, v3).map { dao.getById(it)!! }
        assertEquals(listOf(true, false, false), all.map { it.isOldestVersion })
        assertEquals(listOf(false, false, true), all.map { it.isNewestVersion })

        assertEquals(3, query().size)
        assertEquals(listOf(v1), query(versions = VersionHandling.OLDEST_ONLY).map { it.id })
        assertEquals(listOf(v3), query(versions = VersionHandling.NEWEST_ONLY).map { it.id })
    }

    @Test
    fun sortingAndPackageFilter() = runBlocking {
        val a1 = dao.insert(item(pkg = "a", time = 10, text = "1"))
        val b1 = dao.insert(item(pkg = "b", time = 20, text = "2"))
        val a2 = dao.insert(item(pkg = "a", time = 30, text = "3"))
        assertEquals(listOf(a2, b1, a1), query().map { it.id })
        assertEquals(listOf(a1, b1, a2), query(sorting = NotificationSorting.OLDEST_FIRST).map { it.id })
        assertEquals(listOf(a2, a1), query(pkg = "a").map { it.id })
    }

    @Test
    fun hideDuplicatesKeepsLatestOfEachGroup() = runBlocking {
        dao.insert(item(time = 1))
        val dupLatest = dao.insert(item(time = 2))
        val other = dao.insert(item(time = 3, text = "different"))
        assertEquals(3, query().size)
        assertEquals(listOf(other, dupLatest), query(hideDuplicates = true).map { it.id })
    }

    @Test
    fun appSummaries() = runBlocking {
        dao.insert(item(pkg = "a", time = 10, text = "1"))
        dao.insert(item(pkg = "a", time = 30, text = "2"))
        dao.insert(item(pkg = "b", time = 20))
        val newest = dao.appsNewestFirst().first()
        assertEquals(listOf("a", "b"), newest.map { it.packageName })
        assertEquals(30L, newest[0].lastTime)
        assertEquals(2, newest[0].count)
        assertEquals(listOf("b", "a"), dao.appsOldestFirst().first().map { it.packageName })
    }

    @Test
    fun deleteByAppOlderThanAndAll() = runBlocking {
        dao.insert(item(pkg = "a", time = 10, text = "1"))
        dao.insert(item(pkg = "a", time = 30, text = "2"))
        dao.insert(item(pkg = "b", time = 20))
        dao.deleteOlderThan(20)
        assertEquals(listOf(20L, 30L), query(sorting = NotificationSorting.OLDEST_FIRST).map { it.time })
        dao.deleteByApp("a")
        assertEquals(listOf("b"), query().map { it.packageName })
        dao.deleteAll()
        assertTrue(query().isEmpty())
    }

    @Test
    fun activeNotificationLifecycle() = runBlocking {
        val id1 = dao.insert(item(time = 1))
        assertNull(active.find("a", 5, "tag"))
        active.insert(ActiveNotificationItem(appPackageName = "a", systemId = 5, systemTag = "tag", previousNotificationItemId = id1))
        val found = active.find("a", 5, "tag")!!
        assertEquals(id1, found.previousNotificationItemId)

        val id2 = dao.insert(item(time = 2, text = "u"), previousVersionId = id1)
        active.updateLastId(found.id, id2)
        assertEquals(id2, active.find("a", 5, "tag")!!.previousNotificationItemId)
        assertNull(active.find("a", 5)) // different tag

        active.remove("a", 5, "tag")
        assertNull(active.find("a", 5, "tag"))
    }

    @Test
    fun activeNotificationUniqueKeyReplaces() = runBlocking {
        val id1 = dao.insert(item(time = 1))
        val id2 = dao.insert(item(time = 2, text = "u"))
        active.insert(ActiveNotificationItem(appPackageName = "a", systemId = 1, previousNotificationItemId = id1))
        active.insert(ActiveNotificationItem(appPackageName = "a", systemId = 1, previousNotificationItemId = id2))
        assertEquals(1, active.getAll().size)
        assertEquals(id2, active.find("a", 1)!!.previousNotificationItemId)
    }

    @Test
    fun deletingNotificationCascadesToActive() = runBlocking {
        val id = dao.insert(item(time = 1))
        active.insert(ActiveNotificationItem(appPackageName = "a", systemId = 1, previousNotificationItemId = id))
        assertNotNull(active.find("a", 1))
        dao.deleteByApp("a")
        assertTrue(active.getAll().isEmpty())
        assertFalse(active.find("a", 1) != null)
    }
}
