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
        fun versionClause(prefix: String) = when (versionHandling) {
            VersionHandling.SHOW_ALL -> null
            VersionHandling.OLDEST_ONLY -> "${prefix}is_oldest_version = 1"
            VersionHandling.NEWEST_ONLY -> "${prefix}is_newest_version = 1"
        }
        versionClause("")?.let { where += it }
        maxId?.let { where += "id <= $it" }
        if (hideDuplicates) {
            // Keep a row only if no later row of its duplicate group would also be shown.
            // Written as a correlated lookup (not GROUP BY/MAX) so the time index can drive the
            // result and stop after LIMIT; see QueryPlanTest.
            val later = listOfNotNull(
                "d.duplicate_group_id = notifications.duplicate_group_id",
                "d.id > notifications.id",
                versionClause("d."),
                maxId?.let { "d.id <= $it" },
            )
            where += "NOT EXISTS (SELECT 1 FROM notifications d WHERE ${later.joinToString(" AND ")})"
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
