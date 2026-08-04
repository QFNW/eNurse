package com.enurse.app.di

import com.enurse.app.BuildConfig
import com.enurse.app.data.remote.api.*
import com.enurse.app.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Debug → emulator loopback; Release → production domain (see app/build.gradle.kts)
    private const val BASE_URL = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Debug 看完整 body；release 只留摘要（BODY 会把 APK 下载内容逐包刷进 Logcat）
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton fun provideAuthApi(r: Retrofit): AuthApi = r.create(AuthApi::class.java)
    @Provides @Singleton fun provideBmApi(r: Retrofit): BmApi = r.create(BmApi::class.java)
    @Provides @Singleton fun provideMedicationApi(r: Retrofit): MedicationApi = r.create(MedicationApi::class.java)
    @Provides @Singleton fun provideGroupApi(r: Retrofit): GroupApi = r.create(GroupApi::class.java)
    @Provides @Singleton fun provideAlertApi(r: Retrofit): AlertApi = r.create(AlertApi::class.java)
    @Provides @Singleton fun provideUserApi(r: Retrofit): UserApi = r.create(UserApi::class.java)
}
