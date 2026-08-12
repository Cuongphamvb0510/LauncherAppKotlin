package com.example.launcherappkotlin.data.remote

import android.graphics.Bitmap
import android.graphics.Color
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ThemeServer : NanoHTTPD(8080) {

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {

            "/theme" -> {
                val json = JSONObject()
                    .put("theme", "summer")
                    .put("wallpaper", "http://127.0.0.1:8080/wallpaper.jpg")
                    .toString()

                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    json
                )
            }

            "/wallpaper.jpg" -> {
                // Ảnh demo màu cam (theme summer)
                val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.parseColor("#FF6F00"))

                val output = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                val bytes = output.toByteArray()

                newFixedLengthResponse(
                    Response.Status.OK,
                    "image/jpeg",
                    ByteArrayInputStream(bytes),
                    bytes.size.toLong()
                )
            }

            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "text/plain",
                "404"
            )
        }
    }
}