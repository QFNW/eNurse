package com.enurse.app.data.remote.interceptor

import com.enurse.app.data.local.datastore.TokenDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the Bearer JWT token to every request.
 * On 401, re-reads token from DataStore (may have been refreshed
 * by a parallel login/refresh request) and retries once.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.accessToken.first() }

        val request = attachToken(chain, token)
        val response = chain.proceed(request)

        // On 401, re-read token and retry once (token may have been refreshed)
        if (response.code == 401) {
            val newToken = runBlocking { tokenDataStore.accessToken.first() }
            if (newToken != null && newToken != token) {
                response.close()
                return chain.proceed(attachToken(chain, newToken))
            }
        }

        return response
    }

    private fun attachToken(
        chain: Interceptor.Chain,
        token: String?,
    ) = if (token != null) {
        chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
    } else {
        chain.request()
    }
}
