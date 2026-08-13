package com.example.launcherappkotlin.data.api

import com.example.launcherappkotlin.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cấu hình OkHttp + Retrofit kiểu production.
 * - Timeout rõ ràng
 * - Header chung (Accept / User-Agent)
 * - Logging chỉ bật khi [BuildConfig.DEBUG]
 * - baseUrl từ [BuildConfig.BASE_URL] (debug local / release HTTPS)
 */
object NetworkModule {

    private val gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header(ApiConfig.HEADER_ACCEPT, ApiConfig.MEDIA_TYPE_JSON)
            .header(ApiConfig.HEADER_USER_AGENT, ApiConfig.USER_AGENT)
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    val retrofit: Retrofit by lazy {
        require(BuildConfig.BASE_URL.endsWith("/")) {
            "BASE_URL phải kết thúc bằng '/' (Retrofit relative path)"
        }
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    inline fun <reified T> create(): T = retrofit.create(T::class.java)

    val themeApi: ThemeApi by lazy { create() }
}
