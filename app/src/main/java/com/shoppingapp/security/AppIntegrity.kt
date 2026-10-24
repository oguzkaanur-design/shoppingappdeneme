package com.shoppingapp.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Uygulamanın kendi imzasıyla çalıştığını doğrular.
 *
 * APK başka bir anahtarla yeniden imzalanıp dağıtılırsa (kopyalanmış/modlanmış sürüm)
 * buradaki özet tutmaz ve uygulama açılmaz. Kullanıcıların kurduğu dosyanın gerçekten
 * bizim yayınladığımız APK olduğunu garanti eder; içerik hırsızlığına karşı ilk savunma
 * hattıdır (asıl koruma R8 ile kod karıştırmadır).
 */
object AppIntegrity {

    /** Yayın anahtarının sertifika özeti (SHA-256, büyük/küçük harf duyarsız). */
    const val RELEASE_CERT_SHA256 =
        "a596e8f9c285af6fd1bb4d0b8f49a3619d5e817be772d9d3b2444c8e871a08d2"

    fun isTrusted(context: Context, expected: String = RELEASE_CERT_SHA256): Boolean {
        val actual = signingCertificateSha256(context) ?: return false
        return actual.equals(expected.trim(), ignoreCase = true)
    }

    private fun signingCertificateSha256(context: Context): String? {
        val packageManager = context.packageManager
        val packageName = context.packageName

        val signatures = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = info.signingInfo ?: return null
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                    .signatures
            }
        }.getOrNull() ?: return null

        val certificate = signatures.firstOrNull() ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(certificate.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
