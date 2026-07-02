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
 * On 401, attempts a synchronous token refresh and retries once.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {

    companion object {
        private val JSON = "application/json".toMediaType()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.accessToken.first() }
        val request = attachToken(chain, token)
        val response = chain.proceed(request)

        if (response.code == 401 && token != null) {
            response.close()

            // Attempt synchronous refresh
            val refreshed = runBlocking { tryRefreshToken() }
            if (refreshed != null) {
                return chain.proceed(attachToken(chain, refreshed))
            }
        }

        return response
    }

    /**
     * Synchronously call /auth/refresh and update DataStore.
     * Returns the new access token, or null if refresh failed.
     */
    private suspend fun tryRefreshToken(): String? {
        val refreshToken = tokenDataStore.refreshToken.first() ?: return null

        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val body = Gson().toJson(mapOf("refreshToken" to refreshToken))
            val refreshRequest = Request.Builder()
                .url(getBaseUrl() + "auth/refresh")
                .post(body.toRequestBody(JSON))
                .build()

            val response = client.newCall(refreshRequest).execute()
            if (!response.isSuccessful) {
                response.close()
                return null
            }

            val json = Gson().fromJson(response.body?.string(), Map::class.java)
            response.close()

            val newAccess = json["accessToken"] as? String ?: return null
            val newRefresh = json["refreshToken"] as? String ?: return null

            tokenDataStore.saveTokens(newAccess, newRefresh)
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
