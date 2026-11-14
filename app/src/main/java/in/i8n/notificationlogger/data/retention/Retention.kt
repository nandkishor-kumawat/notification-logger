package `in`.i8n.notificationlogger.data.retention

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import `in`.i8n.notificationlogger.data.db.AppDatabase
import `in`.i8n.notificationlogger.data.db.NotificationDao
import `in`.i8n.notificationlogger.data.settings.SettingsRepository
import java.util.concurrent.TimeUnit

/** Deletes notifications older than the "keep for N days" setting. */
class RetentionCleaner(
    private val dao: NotificationDao,
    private val settings: SettingsRepository,
) {
    /** Returns the number of deleted rows (0 when the setting is "never"). */
    suspend fun run(now: Long = System.currentTimeMillis()): Int {
        val days = settings.current().keepingDays
        if (days <= 0) return 0
        return dao.deleteOlderThan(now - TimeUnit.DAYS.toMillis(days.toLong()))
    }
}

class RetentionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RetentionCleaner(
            AppDatabase.get(applicationContext).notificationDao(),
            SettingsRepository(applicationContext),
        ).run()
        return Result.success()
    }
}

object RetentionScheduler {
    private const val PERIODIC = "retention-daily"
    private const val NOW = "retention-now"

    /** Daily job; keeps an already scheduled one. */
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RetentionWorker>(1, TimeUnit.DAYS).build(),
        )
    }

    /** One run right away (app start, or after the setting changed). */
    fun runNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RetentionWorker>().build(),
        )
    }
}
