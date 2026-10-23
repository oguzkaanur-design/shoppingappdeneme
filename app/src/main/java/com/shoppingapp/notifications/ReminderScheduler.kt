package com.shoppingapp.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Hatırlatıcı işini günde bir kez, akşam 18:00 civarında çalışacak şekilde kurar.
 *
 * Aynı iş adı kullanıldığı için `UPDATE` politikası yeniden çizelgelemede
 * yinelenen iş oluşturmaz; kullanıcı ayarı kapatınca iş tamamen iptal edilir.
 */
object ReminderScheduler {

    private const val WORK_NAME = "einkauf-reminders"

    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var target = now.withHour(18).withMinute(0).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val initialDelay = Duration.between(now, target).toMillis().coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
