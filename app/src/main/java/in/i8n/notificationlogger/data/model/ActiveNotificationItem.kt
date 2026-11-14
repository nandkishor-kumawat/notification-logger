package `in`.i8n.notificationlogger.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Maps a notification currently shown by the system (package + id + tag) to the latest
 * logged version of it, so that updates can be linked together.
 *
 * [systemTag] is non-null ("" = no tag) because SQLite unique indices treat NULLs as distinct.
 */
@Entity(
    tableName = "active_notifications",
    foreignKeys = [
        ForeignKey(
            entity = NotificationItem::class,
            parentColumns = ["id"],
            childColumns = ["previous_notification_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("app_package_name", "system_id", "system_tag", unique = true),
        Index("previous_notification_item_id"),
    ],
)
data class ActiveNotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "app_package_name") val appPackageName: String,
    @ColumnInfo(name = "system_id") val systemId: Int,
    @ColumnInfo(name = "system_tag") val systemTag: String = "",
    @ColumnInfo(name = "previous_notification_item_id") val previousNotificationItemId: Long,
)
