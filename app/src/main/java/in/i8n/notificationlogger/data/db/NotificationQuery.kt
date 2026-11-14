package `in`.i8n.notificationlogger.data.db

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling

/** Builds the filtered/sorted notification list query. [packageName] null = all apps. */
object NotificationQuery {
    fun build(
        packageName: String?,
        sorting: NotificationSorting,
        versionHandling: VersionHandling,
        hideDuplicates: Boolean,
        /** Only rows with id <= this (a stable snapshot for chunked reads). */
        maxId: Long? = null,
        limit: Int? = null,
        offset: Int = 0,
    ): SupportSQLiteQuery {
        val where = mutableListOf<String>()
        val args = mutableListOf<Any>()
        if (packageName != null) {
            where += "package_name = ?"
            args += packageName
        }
        val versionClause = when (versionHandling) {
            VersionHandling.SHOW_ALL -> null
            VersionHandling.OLDEST_ONLY -> "is_oldest_version = 1"
            VersionHandling.NEWEST_ONLY -> "is_newest_version = 1"
        }
        versionClause?.let { where += it }
        maxId?.let { where += "id <= $it" }
        if (hideDuplicates) {
            // Keep only the latest notification of each duplicate group. The version filter is
            // repeated inside so a group is judged by the rows that would otherwise be shown.
            val innerClauses = listOfNotNull(versionClause, maxId?.let { "id <= $it" })
            val inner = if (innerClauses.isEmpty()) "" else " WHERE " + innerClauses.joinToString(" AND ")
            where += "id IN (SELECT MAX(id) FROM notifications$inner GROUP BY duplicate_group_id)"
        }
        val order = when (sorting) {
            NotificationSorting.NEWEST_FIRST -> "time DESC, id DESC"
            NotificationSorting.OLDEST_FIRST -> "time ASC, id ASC"
        }
        val sql = buildString {
            append("SELECT * FROM notifications")
            if (where.isNotEmpty()) append(" WHERE ").append(where.joinToString(" AND "))
            append(" ORDER BY ").append(order)
            if (limit != null) append(" LIMIT ").append(limit).append(" OFFSET ").append(offset)
        }
        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }
}
