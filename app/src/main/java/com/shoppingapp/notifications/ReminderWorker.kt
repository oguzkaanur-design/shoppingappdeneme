package com.shoppingapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shoppingapp.di.BackgroundEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId

/**
 * Günde bir kez çalışan hatırlatıcı işi. Karar [ReminderPlanner]'da verilir;
 * burada yalnızca karar bildirime çevrilir ve aynı hatırlatıcı günde bir kez gösterilir.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!ReminderSettings.isEnabled(context)) return Result.success()
        if (!NotificationHelper.canPost(context)) return Result.success()

        val entryPoint = EntryPointAccessors.fromApplication(context, BackgroundEntryPoint::class.java)
        val now = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()

        val messages = runCatching {
            ReminderPlanner.plan(
                offers = entryPoint.flyerListDao().getAll(),
                pantry = entryPoint.shoppingDao().getPantryRowsOnce(),
                pendingCount = entryPoint.shoppingDao().getPendingCountOnce(),
                nowMillis = now,
                today = today
            )
        }.getOrElse { return Result.success() }

        messages.forEach { message ->
            if (!ReminderSettings.shouldNotifyToday(context, message.key, today)) return@forEach
            NotificationHelper.notify(
                context = context,
                id = message.id,
                title = NotificationHelper.title(context, message),
                text = NotificationHelper.body(context, message),
                route = message.route
            )
            ReminderSettings.markNotifiedToday(context, message.key, today)
        }

        return Result.success()
    }
}
