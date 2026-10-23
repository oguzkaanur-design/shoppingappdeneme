package com.shoppingapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.shoppingapp.MainActivity
import com.shoppingapp.R
import com.shoppingapp.di.BackgroundEntryPoint
import com.shoppingapp.notifications.NotificationHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Ana ekran widget'ı: kaç ürün alınacak, ilk birkaç ürün ve tek dokunuşla
 * barkod tarama / liste kısayolu. Veriler Room'dan okunur, ağ gerekmez.
 */
class ShoppingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val content = loadContent(appContext)
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(appContext, content))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    val manager = AppWidgetManager.getInstance(appContext) ?: return@runCatching
                    val component = ComponentName(appContext, ShoppingWidgetProvider::class.java)
                    val ids = manager.getAppWidgetIds(component)
                    if (ids.isEmpty()) return@runCatching
                    val content = loadContent(appContext)
                    ids.forEach { id -> manager.updateAppWidget(id, buildViews(appContext, content)) }
                }
            }
        }

        private data class WidgetContent(
            val count: Int,
            val lines: List<String>
        )

        private suspend fun loadContent(context: Context): WidgetContent {
            return runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context,
                    BackgroundEntryPoint::class.java
                )
                val dao = entryPoint.shoppingDao()
                WidgetContent(
                    count = dao.getPendingCountOnce(),
                    lines = dao.getPendingPreview(4).map { row ->
                        val qty = if (row.quantity % 1.0 == 0.0) {
                            row.quantity.toInt().toString()
                        } else {
                            String.format(Locale.GERMANY, "%.1f", row.quantity)
                        }
                        "• ${row.name}  ($qty ${row.unit})"
                    }
                )
            }.getOrDefault(WidgetContent(count = 0, lines = emptyList()))
        }

        private fun buildViews(context: Context, content: WidgetContent): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_shopping)

            views.setTextViewText(
                R.id.widget_count,
                if (content.count == 0) "Liste boş" else "${content.count} ürün alınacak"
            )
            views.setTextViewText(
                R.id.widget_items,
                if (content.lines.isEmpty()) {
                    "Barkod tarayarak veya listeden ürün ekle."
                } else {
                    content.lines.joinToString("\n")
                }
            )

            views.setOnClickPendingIntent(
                R.id.widget_open,
                activityIntent(context, requestCode = 10, route = null)
            )
            views.setOnClickPendingIntent(
                R.id.widget_scan,
                activityIntent(context, requestCode = 11, route = "scanner")
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                activityIntent(context, requestCode = 12, route = null)
            )
            return views
        }

        private fun activityIntent(context: Context, requestCode: Int, route: String?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (route != null) putExtra(NotificationHelper.EXTRA_ROUTE, route)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getActivity(context, requestCode, intent, flags)
        }
    }
}
