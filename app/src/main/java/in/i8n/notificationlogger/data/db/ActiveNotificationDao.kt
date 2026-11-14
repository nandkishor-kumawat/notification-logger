package `in`.i8n.notificationlogger.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.i8n.notificationlogger.data.model.ActiveNotificationItem

@Dao
interface ActiveNotificationDao {
    @Query(
        "SELECT * FROM active_notifications WHERE app_package_name = :pkg " +
            "AND system_id = :systemId AND system_tag = :systemTag",
    )
    suspend fun find(pkg: String, systemId: Int, systemTag: String = ""): ActiveNotificationItem?

    @Query("SELECT * FROM active_notifications")
    suspend fun getAll(): List<ActiveNotificationItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ActiveNotificationItem): Long

    @Query("UPDATE active_notifications SET previous_notification_item_id = :notificationId WHERE id = :id")
    suspend fun updateLastId(id: Long, notificationId: Long)

    @Query(
        "DELETE FROM active_notifications WHERE app_package_name = :pkg " +
            "AND system_id = :systemId AND system_tag = :systemTag",
    )
    suspend fun remove(pkg: String, systemId: Int, systemTag: String = "")
}
