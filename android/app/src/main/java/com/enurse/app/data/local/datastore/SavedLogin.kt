package com.enurse.app.data.local.datastore

/**
 * One saved login account (username or email). Password is stored in plaintext
 * on device only when the user opted in with "记住密码" — deliberately no
 * encryption, per product decision. Serialized to JSON via Gson.
 */
data class SavedLogin(
    val login: String,
    val password: String? = null,
    val lastLoginAt: Long = 0L
)
