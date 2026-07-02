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
 * Uses runBlocking for the DataStore read — acceptable because
 * DataStore reads are fast (~1ms) and OkHttp interceptors are synchronous.
 * Token refresh on 401 is handled by the repository layer.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.accessToken.first() }

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
