package com.shoppingapp.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import java.io.File

/**
 * Çalışma ortamındaki kurcalama izlerini toplar.
 *
 * Bu kontroller *kanıt* değil, sinyaldir: köklenmiş bir cihazda meşru kullanıcı da
 * olabilir. Bu yüzden yalnızca ücretli özellik kapısında kullanılır; normal kullanım
 * engellenmez. Asıl güvence, lisansın özel anahtarla imzalanması ve APK imza
 * doğrulamasıdır.
 */
object TamperSignals {

    private val hookMarkers = listOf("frida", "xposed", "lsposed", "substrate", "riru")

    fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("vbox") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            product.contains("sdk_gphone") ||
            product.contains("google_sdk") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("vbox")
    }

    /** Frida/Xposed gibi enjeksiyon araçlarının süreç içi izlerini arar. */
    fun hasHookingFramework(): Boolean {
        if (hookFiles.any { File(it).exists() }) return true
        val maps = runCatching { File("/proc/self/maps").readLines() }.getOrNull() ?: return false
        return maps.any { line ->
            val lower = line.lowercase()
            hookMarkers.any { lower.contains(it) }
        }
    }

    /** Kök izleri bilgi amaçlıdır; premium kapısında tek başına engel sayılmaz. */
    fun hasRootArtifacts(): Boolean = suPaths.any { File(it).exists() }

    /** Ücretli lisansın verilemeyeceği ortamlar. */
    fun isCompromised(context: Context): Boolean =
        isDebuggable(context) || isEmulator() || hasHookingFramework()

    private val hookFiles = listOf(
        "/system/framework/XposedBridge.jar",
        "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so",
        "/data/local/tmp/frida-server",
        "/data/local/tmp/re.frida.server"
    )

    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/app/Superuser.apk",
        "/data/adb/magisk"
    )
}
