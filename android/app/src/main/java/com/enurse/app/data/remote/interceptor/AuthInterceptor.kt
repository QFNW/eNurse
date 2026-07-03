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
 * Token refresh on 401 is handled by the ViewModel/repository layer.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.accessToken.first() }
        val request = attachToken(chain, token)
        return chain.proceed(request)
    }

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
