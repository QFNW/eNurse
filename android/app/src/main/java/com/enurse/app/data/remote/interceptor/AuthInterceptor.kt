package com.enurse.app.data.remote.interceptor

import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.remote.api.AuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the access token, and on 401 transparently refreshes it once via
 * POST auth/refresh (single-flight: concurrent 401s share one refresh), then
 * replays the original request. If refresh fails, the token is cleared, which
 * triggers the NavGraph logout redirect via the DataStore flow.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    // Provider breaks the OkHttpClient → AuthInterceptor → AuthApi → Retrofit → OkHttpClient
    // dependency cycle: the actual API is resolved lazily at refresh time.
    private val authApiProvider: javax.inject.Provider<AuthApi>
) : Interceptor {

    private val cacheLock = Mutex()
    private var refreshJob: Deferred<Boolean>? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = runBlocking { tokenDataStore.accessToken.first() }
        val authed = if (token != null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original

        val response = chain.proceed(authed)
        if (response.code != 401 || isAuthEndpoint(original.url.encodedPath)) return response

        return runBlocking {
            if (refreshOnce()) {
                // 401 body is consumed here; replay with the fresh token
                response.close()
                val newToken = tokenDataStore.accessToken.first()
                val retried = chain.proceed(
                    original.newBuilder().header("Authorization", "Bearer $newToken").build()
                )
                if (retried.code == 401) {
                    // Still 401 after a successful refresh — token is genuinely invalid
                    tokenDataStore.clear()
                }
                retried
            } else {
                tokenDataStore.clear()
                response
            }
        }
    }

    private fun isAuthEndpoint(path: String): Boolean =
        path.endsWith("auth/login") || path.endsWith("auth/register") || path.endsWith("auth/refresh")

    /** Single-flight refresh: concurrent 401s await the same Deferred. */
    private suspend fun refreshOnce(): Boolean {
        val job = cacheLock.withLock {
            refreshJob ?: coroutineScope {
                async(Dispatchers.IO) { doRefresh() }
            }.also { refreshJob = it }
        }
        val ok = job.await()
        cacheLock.withLock { if (refreshJob === job) refreshJob = null }
        return ok
    }

    private suspend fun doRefresh(): Boolean {
        val refresh = tokenDataStore.refreshToken.first() ?: return false
        return runCatching {
            val res = authApiProvider.get().refresh(mapOf("refreshToken" to refresh))
            tokenDataStore.saveTokens(res.accessToken, res.refreshToken)
            true
        }.getOrDefault(false)
    }
}
