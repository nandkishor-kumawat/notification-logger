package `in`.i8n.notificationlogger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import `in`.i8n.notificationlogger.data.model.ActiveNotificationItem
import `in`.i8n.notificationlogger.data.model.NotificationItem

@Database(
    entities = [NotificationItem::class, ActiveNotificationItem::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun activeNotificationDao(): ActiveNotificationDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "notifications.db",
            ).build().also { instance = it }
        }
    }
}
