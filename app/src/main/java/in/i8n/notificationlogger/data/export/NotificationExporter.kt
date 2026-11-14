package `in`.i8n.notificationlogger.data.export

import `in`.i8n.notificationlogger.data.db.NotificationDao
import `in`.i8n.notificationlogger.data.db.NotificationQuery
import `in`.i8n.notificationlogger.data.model.NotificationSorting
import `in`.i8n.notificationlogger.data.model.VersionHandling

/**
 * Writes notifications as plain text, using the same query as the on-screen list. Reads the
 * database in chunks, and only up to the newest row that existed when the export started, so
 * notifications arriving meanwhile can't cause skipped or repeated entries.
 *
 * Each entry:
 * ```
 * title
 * text
 * [App - ]date
 *
 * ```
 */
class NotificationExporter(
    private val dao: NotificationDao,
    private val chunkSize: Int = 50,
) {
    /** [packageName] null = all apps (each entry then names its app). Returns the entry count. */
    suspend fun export(
        out: Appendable,
        packageName: String?,
        sorting: NotificationSorting,
        versionHandling: VersionHandling,
        hideDuplicates: Boolean,
        appLabel: (String) -> String,
        formatTime: (Long) -> String,
        header: String? = null,
    ): Int {
        if (header != null) out.append(header).append("\n\n")
        val snapshotMaxId = dao.maxId()
        var offset = 0
        var count = 0
        while (true) {
            val chunk = dao.list(
                NotificationQuery.build(
                    packageName, sorting, versionHandling, hideDuplicates,
                    maxId = snapshotMaxId, limit = chunkSize, offset = offset,
                ),
            )
            for (n in chunk) {
                n.title?.let { out.append(it).append('\n') }
                n.text?.let { out.append(it).append('\n') }
                if (packageName == null) out.append(appLabel(n.packageName)).append(" - ")
                out.append(formatTime(n.time)).append("\n\n")
                count++
            }
            if (chunk.size < chunkSize) break
            offset += chunkSize
        }
        return count
    }
}
