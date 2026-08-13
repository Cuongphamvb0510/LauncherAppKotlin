package com.example.launcherappkotlin.data.api

/**
 * Hằng số network dùng chung (timeout, header).
 * Base URL lấy từ [com.example.launcherappkotlin.BuildConfig.BASE_URL].
 */
object ApiConfig {
    const val CONNECT_TIMEOUT_SEC = 15L
    const val READ_TIMEOUT_SEC = 30L
    const val WRITE_TIMEOUT_SEC = 30L

    const val HEADER_ACCEPT = "Accept"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_USER_AGENT = "User-Agent"

    const val MEDIA_TYPE_JSON = "application/json"
    const val USER_AGENT = "LauncherAppKotlin/1.0 (Android)"
}
