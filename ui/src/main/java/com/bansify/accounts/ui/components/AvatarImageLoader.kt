package com.bansify.accounts.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

internal object AvatarImageLoader {
    private const val MAX_AVATAR_BYTES = 2 * 1024 * 1024
    private val cache = LruCache<String, ImageBitmap>(16)

    // Dedicated public download client — NEVER uses GothwadAccounts.authInterceptor!
    private val publicImageClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun load(url: String?): ImageBitmap? {
        if (url.isNullOrBlank() || !url.startsWith("https://")) return null
        cache.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).get().build()
                publicImageClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body ?: return@use null
                    if (body.contentLength() > MAX_AVATAR_BYTES) return@use null
                    val bytes = body.bytes()
                    if (bytes.isEmpty() || bytes.size > MAX_AVATAR_BYTES) return@use null
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@use null
                    val bitmap = decoded.asImageBitmap()
                    cache.put(url, bitmap)
                    bitmap
                }
            }.getOrNull()
        }
    }
}

@Composable
internal fun rememberAvatarBitmap(avatarUrl: String?): ImageBitmap? {
    var bitmap by remember(avatarUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(avatarUrl) {
        bitmap = AvatarImageLoader.load(avatarUrl)
    }
    return bitmap
}
