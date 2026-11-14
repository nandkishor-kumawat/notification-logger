package `in`.i8n.notificationlogger

import android.app.Application
import `in`.i8n.notificationlogger.data.retention.RetentionScheduler

class NotificationLoggerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetentionScheduler.schedule(this)
        RetentionScheduler.runNow(this)
    }
}
