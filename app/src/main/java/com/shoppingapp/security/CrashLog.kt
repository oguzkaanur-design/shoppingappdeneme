package com.shoppingapp.security

import android.content.Context
import android.os.Build
import android.os.Process
import com.shoppingapp.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Yakalanmamış hataları cihazda tutar. Üçüncü taraf bir çökme servisi
 * kullanılmadığı için hiçbir veri ağa çıkmaz; kayıt yalnızca kullanıcı
 * "Tanılama kaydını paylaş" dediğinde paylaşılır.
 *
 * Kayıt uygulama sürümü, Android sürümü, iş parçacığı adı ve yığın izini
 * içerir; kullanıcı verisi (liste, fiyat, market) yazılmaz. Boyut 64 KB ile
 * sınırlıdır, böylece art arda gelen çökmeler diskte büyümez.
 */
object CrashLog {

    private const val DIR = "crash"
    private const val FILE = "last-crash.txt"
    private const val MAX_CHARS = 64 * 1024

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    /** Kayıt yoksa null döner; paylaşım ekranı bunu kullanıcıya bildirir. */
    fun read(context: Context): String? = runCatching {
        val file = file(context)
        if (!file.isFile) return null
        file.readText().take(MAX_CHARS).takeIf { it.isNotBlank() }
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun file(context: Context) = File(File(context.filesDir, DIR), FILE)

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val directory = File(context.filesDir, DIR).apply { mkdirs() }
        val stackTrace = StringWriter().also { writer ->
            PrintWriter(writer).use { error.printStackTrace(it) }
        }.toString()

        val text = buildString {
            append("Korb ").append(BuildConfig.VERSION_NAME)
            append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
            append("Android ").append(Build.VERSION.RELEASE)
            append(" / API ").append(Build.VERSION.SDK_INT).append('\n')
            append("Thread: ").append(thread.name).append('\n')
            append(TIMESTAMP.format(Date())).append("\n\n")
            append(stackTrace)
        }.take(MAX_CHARS)

        File(directory, FILE).writeText(text)
    }

    private val TIMESTAMP = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
}
