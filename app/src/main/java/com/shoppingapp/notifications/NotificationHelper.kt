package com.shoppingapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shoppingapp.MainActivity
import com.shoppingapp.R

/** Hatırlatıcı bildirimlerinin kanalı ve gönderimi tek yerde. */
object NotificationHelper {

    const val CHANNEL_ID = "einkauf_reminders"
    const val EXTRA_ROUTE = "com.shoppingapp.extra.ROUTE"

    const val ID_EXPIRING_OFFERS = 1001
    const val ID_LOW_STOCK = 1002
    const val ID_WEEKLY_SHOPPING = 1003

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alışveriş hatırlatıcıları",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Kilerde azalan ürünler, süresi dolan fırsatlar ve haftalık liste hatırlatması"
        }
        manager.createNotificationChannel(channel)
    }

    /** Android 13+ için bildirim izni verilmiş mi? */
    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    /**
     * @param route bildirime dokununca açılacak ekran ("pantry", "flyerList" …).
     */
    fun notify(context: Context, id: Int, title: String, text: String, route: String? = null) {
        if (!canPost(context)) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(EXTRA_ROUTE, route)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(context, id, intent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
