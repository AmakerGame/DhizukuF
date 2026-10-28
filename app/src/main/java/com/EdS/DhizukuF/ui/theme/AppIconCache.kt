package com.EdS.DhizukuF.ui.theme

import android.app.admin.DeviceAdminInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object AppIconCache : KoinComponent {
    private val context by inject<Context>()

    private val defaultImageBitMap: ImageBitmap by lazy {
        val drawable = ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)
            ?: context.packageManager.defaultActivityIcon
        try {
            drawable.toBitmap().asImageBitmap()
        } catch (_: Exception) {
            // Last resort: 1x1 transparent — UI still has a non-null ImageBitmap.
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }

    private val lruCache by lazy {
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()

        object : LruCache<String, Bitmap>(availableCacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return (value.byteCount / 1024).coerceAtLeast(1)
            }
        }
    }

    private fun cacheGet(key: String): Bitmap? = synchronized(lruCache) { lruCache[key] }

    private fun cachePut(key: String, bitmap: Bitmap): Bitmap =
        synchronized(lruCache) {
            lruCache.put(key, bitmap)
            bitmap
        }

    private fun loadBitmap(key: String, load: () -> Bitmap?): Bitmap? {
        cacheGet(key)?.let { return it }
        return try {
            val loaded = load() ?: return null
            // Another thread may have filled the cache while we decoded.
            synchronized(lruCache) {
                lruCache[key] ?: loaded.also { lruCache.put(key, it) }
            }
        } catch (_: Exception) {
            null
        }
    }

    @Composable
    fun rememberImageBitmapState(
        applicationInfo: ApplicationInfo
    ): MutableState<ImageBitmap> {
        val key = "${applicationInfo.packageName}_${applicationInfo.uid}"
        val state = remember(key) {
            mutableStateOf(defaultImageBitMap)
        }

        LaunchedEffect(key) {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmap(key) {
                    applicationInfo.loadIcon(context.packageManager)?.toBitmap()
                }
            }
            bitmap?.let { state.value = it.asImageBitmap() }
        }

        return state
    }

    @Composable
    fun rememberImageBitmapState(
        admin: DeviceAdminInfo
    ): MutableState<ImageBitmap> {
        val key =
            "${admin.component.flattenToShortString()}_${admin.activityInfo.applicationInfo.uid}"

        val state = remember(key) {
            mutableStateOf(defaultImageBitMap)
        }

        LaunchedEffect(key) {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmap(key) {
                    admin.loadIcon(context.packageManager)?.toBitmap()
                }
            }
            bitmap?.let { state.value = it.asImageBitmap() }
        }

        return state
    }

    @Composable
    fun rememberImageBitmapState(
        packageName: String,
        drawable: Drawable
    ): MutableState<ImageBitmap> {
        val key = packageName
        val state = remember(key) {
            mutableStateOf(defaultImageBitMap)
        }

        LaunchedEffect(key) {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmap(key) { drawable.toBitmap() }
            }
            bitmap?.let { state.value = it.asImageBitmap() }
        }

        return state
    }
}
