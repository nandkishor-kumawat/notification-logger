package `in`.i8n.notificationlogger.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One logged notification (a single version of it, if the app updated it in place). */
@Entity(
    tableName = "notifications",
    indices = [
        Index("time"),
        Index("package_name", "time"),
        Index("is_oldest_version"),
        Index("is_newest_version"),
        Index("duplicate_group_id"),
    ],
)
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    val time: Long,
    val title: String? = null,
    val text: String? = null,
    val progress: Int = 0,
    @ColumnInfo(name = "progress_max") val progressMax: Int = 0,
    @ColumnInfo(name = "progress_indeterminate") val progressIndeterminate: Boolean = false,
    @ColumnInfo(name = "is_oldest_version") val isOldestVersion: Boolean = true,
    @ColumnInfo(name = "is_newest_version") val isNewestVersion: Boolean = true,
    /** Id of the first notification of a run of identical ones; equals [id] if not a duplicate. */
    @ColumnInfo(name = "duplicate_group_id") val duplicateGroupId: Long = 0,
)
