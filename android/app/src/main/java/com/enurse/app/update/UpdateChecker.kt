package com.enurse.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.enurse.app.BuildConfig
import com.google.gson.Gson
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 版本检查与 APK 下载安装（静态文件分发：GET /apk/version.json → 下载 apkUrl → 安装）。
 * version.json 的 URL 从 BuildConfig.API_BASE_URL 推导（release = https://bohack26.kdns.fr/api/v1/ → /apk/）。
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    private const val APK_FILE_NAME = "app-release.apk"

    /** 最近一次失败的具体原因（诊断用；成功或未检查时为 null）。 */
    @Volatile
    var lastError: String? = null
        private set

    fun versionJsonUrl(): String =
        BuildConfig.API_BASE_URL.removeSuffix("api/v1/") + "apk/version.json"

    /** 请求失败或解析失败返回 null。注意：调用方在 Main dispatcher（rememberCoroutineScope），必须切 IO。 */
    suspend fun checkVersion(context: Context): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val client = entryPoint(context).okHttpClient()
            val req = Request.Builder().url(versionJsonUrl()).build()
            val json = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    lastError = "HTTP ${resp.code}"
                    Log.w(TAG, "checkVersion HTTP ${resp.code}")
                    return@withContext null
                }
                resp.body?.string() ?: return@withContext null
            }
            lastError = null
            Gson().fromJson(json, VersionInfo::class.java)
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            Log.w(TAG, "checkVersion failed", e)
            null
        }
    }

    /** 流式下载 APK 到 cacheDir/update/ 并拉起系统安装器。成功返回 true。 */
    suspend fun downloadAndInstall(context: Context, info: VersionInfo, onProgress: (Float) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "update").apply { mkdirs() }
                val file = File(dir, APK_FILE_NAME)
                file.delete()

                val client = entryPoint(context).okHttpClient()
                val req = Request.Builder().url(info.apkUrl).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        lastError = "HTTP ${resp.code}"
                        Log.w(TAG, "download HTTP ${resp.code}")
                        return@withContext false
                    }
                    val total = resp.body?.contentLength() ?: -1L
                    resp.body?.byteStream()?.use { input ->
                        FileOutputStream(file).use { output ->
                            val buf = ByteArray(8192)
                            var written = 0L
                            while (true) {
                                val read = input.read(buf)
                                if (read == -1) break
                                output.write(buf, 0, read)
                                written += read
                                if (total > 0) onProgress(written.toFloat() / total)
                            }
                        }
                    }
                }
                install(context, file)
                true
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.message}"
                Log.w(TAG, "downloadAndInstall failed", e)
                false
            }
        }

    private fun install(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun entryPoint(context: Context): UpdateEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, UpdateEntryPoint::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UpdateEntryPoint {
        fun okHttpClient(): OkHttpClient
    }
}
