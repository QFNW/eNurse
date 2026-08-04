package com.enurse.app.update

/** 服务器 /apk/version.json 的版本信息（Gson 反序列化）。 */
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String? = null
)
