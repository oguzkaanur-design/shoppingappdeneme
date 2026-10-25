package com.shoppingapp.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Uygulamanın kendi imzasıyla çalıştığını doğrular.
 *
 * Klonlanmış/modlanmış bir APK başka bir anahtarla imzalanır; bu durumda imza özeti
 * listede bulunmaz ve [isTrusted] `false` döner. Bu bilgi yalnızca ücretli özellik
 * kapısında kullanılır: kullanıcı uygulamayı çalıştırmaya devam eder, verisini
 * kaybetmez, ama premium açılmaz.
 *
 * İki imza güvenilirdir:
 * 1. Bu depodaki yükleme/yayın anahtarı — doğrudan APK ve Play dışı dağıtım.
 * 2. Play App Signing anahtarı — Play'den kurulan her sürüm Google tarafından
 *    yeniden imzalanır, yani cihazdaki sertifika bu olur.
 *
 * Play App Signing parmak izi ilk AAB yüklemesinden sonra Play Console →
 * "App integrity" → "App signing key certificate" altında görünür. Değeri
 * [PLAY_APP_SIGNING_CERT_SHA256] sabitine yazın (iki nokta üst üste işaretleri
 * olmadan da kabul edilir).
 */
object AppIntegrity {

    /** Yayın (yükleme) anahtarının sertifika özeti — SHA-256, onaltılı. */
    const val RELEASE_CERT_SHA256 =
        "a596e8f9c285af6fd1bb4d0b8f49a3619d5e817be772d9d3b2444c8e871a08d2"

    /**
     * Play App Signing sertifikası. İlk yüklemeden sonra Play Console'dan alınan
     * değer buraya yazılır; boş bırakılırsa yalnızca [RELEASE_CERT_SHA256] geçerli olur.
     */
    const val PLAY_APP_SIGNING_CERT_SHA256 = ""

    /** Kabul edilen sertifika özetleri (karşılaştırma normalize edilir). */
    val trustedFingerprints: Set<String> get() = setOfNotNull(
        RELEASE_CERT_SHA256.takeIf { it.isNotBlank() },
        PLAY_APP_SIGNING_CERT_SHA256.takeIf { it.isNotBlank() }
    )

    fun isTrusted(
        context: Context,
        trusted: Collection<String> = trustedFingerprints
    ): Boolean {
        val actual = signingCertificateSha256(context) ?: return false
        return trusted.any { normalize(it) == actual }
    }

    /** "A5:96:E8:…" ve "a596e8…" aynı sayılır. */
    internal fun normalize(fingerprint: String): String =
        fingerprint.filter { it.isLetterOrDigit() }.lowercase()

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
