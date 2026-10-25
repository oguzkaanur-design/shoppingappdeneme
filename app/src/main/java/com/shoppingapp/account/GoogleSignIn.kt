package com.shoppingapp.account

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.shoppingapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val email: String?, val displayName: String?) : GoogleSignInResult

    /** Derlemede `googleWebClientId` verilmedi; düğme bilgilendirme gösterir. */
    data object NotConfigured : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

/**
 * Google ile giriş.
 *
 * Kimlik Credential Manager üzerinden alınır (`googleid`), senkronizasyon için
 * gereken Drive izni ayrıca `AuthorizationClient` ile istenir. Her ikisi de
 * Google Cloud'da tanımlı bir **Web** OAuth istemcisi ister; kimlik yoksa
 * akış sessizce [GoogleSignInResult.NotConfigured] döner ve uygulama hesapsız
 * çalışmaya devam eder.
 */
object GoogleSignIn {

    /** Drive'ın yalnızca uygulamaya ait gizli klasörü; başka dosyalara erişim yok. */
    const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    fun isConfigured(): Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun signIn(context: Context): GoogleSignInResult {
        if (!isConfigured()) return GoogleSignInResult.NotConfigured
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val token = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(
                    email = token.id,
                    displayName = token.displayName ?: token.givenName
                )
            } else {
                GoogleSignInResult.Failed("Beklenmeyen kimlik bilgisi türü")
            }
        } catch (cancelled: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (error: GetCredentialException) {
            GoogleSignInResult.Failed(error.message ?: error.type)
        } catch (error: Exception) {
            GoogleSignInResult.Failed(error.message ?: "Google girişi başarısız")
        }
    }

    /**
     * Drive uygulama klasörü için kısa ömürlü erişim belirteci. Kullanıcı izni
     * daha önce verdiyse sessizce yenilenir; verilmediyse onay ekranı açılır.
     */
    suspend fun driveAccessToken(activity: Activity): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("GOOGLE_NOT_CONFIGURED"))
        }
        runCatching {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
                .build()
            val client = Identity.getAuthorizationClient(activity)
            val result = Tasks.await(client.authorize(request))
            result.accessToken
                ?: throw IllegalStateException(
                    result.pendingIntent?.let { "CONSENT_REQUIRED" } ?: "NO_TOKEN"
                )
        }
    }

    /** `ApiException` durum kodunu okunabilir hale getirir (hata ayıklama için). */
    fun describe(error: Throwable): String = when (error) {
        is ApiException -> "Google hatası (${error.statusCode})"
        else -> error.message ?: error.javaClass.simpleName
    }
}
