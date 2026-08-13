package com.example.launcherappkotlin.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.example.launcherappkotlin.data.api.NetworkModule
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object  ImageFileHelper {
    // copy ảnh từ Uri gallery → File nội bộ
    fun copyToInternal(context: Context, uri: Uri, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadDrawable(context: Context, path: String): Drawable? {
        return try {
            val bitmap = BitmapFactory.decodeFile(path) ?: return null
            BitmapDrawable(context.resources, bitmap)
        } catch (e: Exception) {
            null
        }
    }

    /** Tải ảnh qua OkHttp (cùng client/timeout/logging với Retrofit). */
    fun downloadFromUrl(url: String, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            val request = Request.Builder().url(url).get().build()
            NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                FileOutputStream(dest).use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
