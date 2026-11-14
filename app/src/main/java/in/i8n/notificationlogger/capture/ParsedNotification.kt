package `in`.i8n.notificationlogger.capture

/** The loggable content of a posted notification. */
data class ParsedNotification(
    val packageName: String,
    val time: Long,
    val title: String?,
    val text: String?,
    val progress: Int = 0,
    val progressMax: Int = 0,
    val progressIndeterminate: Boolean = false,
)
