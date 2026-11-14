package `in`.i8n.notificationlogger.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import `in`.i8n.notificationlogger.data.model.AppSummary
import `in`.i8n.notificationlogger.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NotificationDao {

    @Insert
    protected abstract suspend fun insertRaw(item: NotificationItem): Long

    @Query("UPDATE notifications SET duplicate_group_id = :groupId WHERE id = :id")
    protected abstract suspend fun setDuplicateGroup(id: Long, groupId: Long)

    @Query("UPDATE notifications SET is_newest_version = 0 WHERE id = :id")
    protected abstract suspend fun markNotNewest(id: Long)

    @Query("SELECT * FROM notifications WHERE package_name = :packageName ORDER BY id DESC LIMIT 1")
    abstract suspend fun getLastOfApp(packageName: String): NotificationItem?

    @Query("SELECT * FROM notifications WHERE id = :id")
    abstract suspend fun getById(id: Long): NotificationItem?

    /**
     * Inserts [item]. If the previous notification of the same app has identical title and
     * text it joins that duplicate group, otherwise it starts its own. Returns the new id.
     *
     * If [previousVersionId] is given, [item] is an update of that notification: the old one
     * stops being the newest version and the new one is not the oldest.
     */
    @Transaction
    open suspend fun insert(item: NotificationItem, previousVersionId: Long? = null): Long {
        val last = getLastOfApp(item.packageName)
        val id = insertRaw(
            item.copy(
                id = 0,
                isOldestVersion = previousVersionId == null,
                isNewestVersion = true,
                duplicateGroupId = 0,
            ),
        )
        val isDuplicate = last != null && last.title == item.title && last.text == item.text
        setDuplicateGroup(id, if (isDuplicate) last!!.duplicateGroupId else id)
        if (previousVersionId != null) markNotNewest(previousVersionId)
        return id
    }

    @RawQuery(observedEntities = [NotificationItem::class])
    protected abstract fun pagedRaw(query: SupportSQLiteQuery): PagingSource<Int, NotificationItem>

    @RawQuery
    abstract suspend fun list(query: SupportSQLiteQuery): List<NotificationItem>

    @Query("SELECT COALESCE(MAX(id), 0) FROM notifications")
    abstract suspend fun maxId(): Long

    fun paged(query: SupportSQLiteQuery): PagingSource<Int, NotificationItem> = pagedRaw(query)

    @Query(
        "SELECT package_name AS packageName, MAX(time) AS lastTime, COUNT(*) AS count " +
            "FROM notifications GROUP BY package_name ORDER BY lastTime DESC",
    )
    abstract fun appsNewestFirst(): Flow<List<AppSummary>>

    @Query(
        "SELECT package_name AS packageName, MAX(time) AS lastTime, COUNT(*) AS count " +
            "FROM notifications GROUP BY package_name ORDER BY lastTime ASC",
    )
    abstract fun appsOldestFirst(): Flow<List<AppSummary>>

    @Query("DELETE FROM notifications WHERE package_name = :packageName")
    abstract suspend fun deleteByApp(packageName: String)

    @Query("DELETE FROM notifications WHERE time < :timestamp")
    abstract suspend fun deleteOlderThan(timestamp: Long): Int

    @Query("DELETE FROM notifications")
    abstract suspend fun deleteAll()
}
