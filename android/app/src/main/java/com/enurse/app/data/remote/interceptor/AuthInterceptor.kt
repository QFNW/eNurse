package com.enurse.app.data.remote.interceptor

import com.enurse.app.data.local.datastore.TokenDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the Bearer JWT token to every request.
 * On 401, attempts a synchronous token refresh and retries once (guarded by X-Retry).
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {

    companion object {
        private val JSON = "application/json".toMediaType()
    }

    // Single shared HTTP client for token refresh (not created per-401)
    private val refreshClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.accessToken.first() }
        val request = attachToken(chain, token)
        val response = chain.proceed(request)

        // Only retry once — guard against infinite loop on permanent 401
        if (response.code == 401 && token != null && chain.request().header("X-Retry") == null) {
            response.close()

            val refreshed = tryRefreshToken()
            if (refreshed != null) {
                val retryRequest = attachToken(chain, refreshed).newBuilder()
                    .header("X-Retry", "1")
                    .build()
                return chain.proceed(retryRequest)
            }
            return chain.proceed(chain.request())
        }

        return response
    }

    /**
     * Synchronously call /auth/refresh and update DataStore.
     * Not a suspend function — uses runBlocking for DataStore reads only.
     */
    private fun tryRefreshToken(): String? {
        val refreshToken = runBlocking { tokenDataStore.refreshToken.first() } ?: return null

        return try {
            val body = Gson().toJson(mapOf("refreshToken" to refreshToken))
            val refreshRequest = Request.Builder()
                .url(getBaseUrl() + "auth/refresh")
                .post(body.toRequestBody(JSON))
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            val json = refreshResponse.use { resp ->
                if (!resp.isSuccessful) return null
                val bodyStr = resp.body?.string() ?: return null
                Gson().fromJson(bodyStr, Map::class.java)
            }

            val newAccess = json["accessToken"] as? String ?: return null
            val newRefresh = json["refreshToken"] as? String ?: return null

            runBlocking { tokenDataStore.saveTokens(newAccess, newRefresh) }
            newAccess
        } catch (_: Exception) {
            null
        }
    }

    private fun getBaseUrl(): String = "http://10.0.2.2:3000/api/v1/"

    private fun attachToken(
        chain: Interceptor.Chain,
        token: String?,
    ) = if (token != null) {
        chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    } else {
        chain.request()
    }
}
