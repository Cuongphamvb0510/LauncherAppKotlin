package com.example.launcherappkotlin.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object ImageFileHelper {
    // copy ảnh từ Uri gallery → File nội bộ
    fun copyToInternal(context: Context, uri: Uri, dest: File): Boolean {
        return try {
            // Tạo folder cha nếu chưa có (vd: files/icons/)
            dest.parentFile?.mkdirs()
            // Mở stream đọc từ gallery
            context.contentResolver.openInputStream(uri)?.use { input ->
                // Ghi ra file nội bộ
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: return false  // openInputStream trả null = thất bại
            true  // copy thành công
        } catch(e: Exception) {
            false
        }
    }
    // đọc file → Drawable để gắn vào ImageView
    fun loadDrawable(context: Context, path: String): Drawable? {
        return try {
            val bitmap = BitmapFactory.decodeFile(path) ?: return null
            BitmapDrawable(context.resources, bitmap)
        } catch (e: Exception) {
            null
        }
    }

    fun downloadFromUrl(url: String, dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            URL(url).openStream().use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}