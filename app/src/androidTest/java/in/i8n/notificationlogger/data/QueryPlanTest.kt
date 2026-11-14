package `in`.i8n.notificationlogger.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.db.NotificationQuery
import `in`.i8n.notificationlogger.data.model.NotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The list queries must be answered from indexes, never by scanning and sorting every row. */
@RunWith(AndroidJUnit4::class)
class QueryPlanTest {
    @Test
    fun listQueriesDoNotSortOrScanWholeTable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.notificationDao()
            // Realistic statistics: planners choose differently on an empty table.
            for (i in 1..3000) {
                dao.insert(NotificationItem(packageName = "p${i % 15}", time = i.toLong(), title = "t${i % 40}", text = "x"))
            }
            db.openHelper.writableDatabase.execSQL("ANALYZE")
            val problems = mutableListOf<String>()
            for (pkg in listOf(null, "p1")) for (sort in NotificationSorting.entries)
                for (versions in VersionHandling.entries) for (hide in listOf(false, true)) {
                    val q = NotificationQuery.build(pkg, sort, versions, hide, limit = 40)
                    val args = if (pkg != null) arrayOf<Any>(pkg) else emptyArray()
                    val plan = buildList {
                        db.openHelper.writableDatabase.query(SimpleSQLiteQuery("EXPLAIN QUERY PLAN ${q.sql}", args)).use { c ->
                            while (c.moveToNext()) add(c.getString(c.getColumnCount() - 1))
                        }
                    }
                    val label = "pkg=$pkg $sort $versions hide=$hide"
                    Log.i("QueryPlan", "$label -> $plan")
                    if (plan.any { it.contains("TEMP B-TREE") && it.contains("ORDER BY") }) problems += "$label sorts: $plan"
                }
            assertTrue(problems.joinToString("\n"), problems.isEmpty())
        } finally {
            db.close()
        }
    }
}
