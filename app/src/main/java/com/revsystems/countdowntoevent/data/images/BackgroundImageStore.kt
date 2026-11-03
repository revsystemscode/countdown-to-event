package com.revsystems.countdowntoevent.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/** Copies user-picked photos into app storage, so they survive even if the original is deleted. */
interface BackgroundImageStore {
    /** Imports [source] (a content:// URI) and returns the stored image's URI. */
    suspend fun import(source: String): String

    suspend fun delete(uri: String)

    /** Bytes of a stored image (for backups), or null if it's missing. */
    suspend fun read(uri: String): ByteArray?

    /** Stores image bytes from a backup. Re-encodes them, so only real images end up on disk. */
    suspend fun importBytes(bytes: ByteArray): String

    /** Removes stored images that no event references (e.g. after a cancelled edit). */
    suspend fun deleteUnused(keep: Set<String>)
}

@Singleton
class FileBackgroundImageStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BackgroundImageStore {
        private val dir: File
            get() = File(context.filesDir, "backgrounds").apply { mkdirs() }

        override suspend fun import(source: String): String =
            withContext(Dispatchers.IO) {
                writeJpeg(decodeScaled(source.toUri()))
            }

        override suspend fun read(uri: String): ByteArray? =
            withContext(
                Dispatchers.IO,
            ) { ownedFile(uri)?.takeIf { it.exists() }?.readBytes() }

        override suspend fun importBytes(bytes: ByteArray): String =
            withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0) throw IOException("Not an image")
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
                val bitmap =
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                        ?: throw IOException("Not an image")
                writeJpeg(bitmap)
            }

        override suspend fun delete(uri: String) {
            withContext(Dispatchers.IO) { ownedFile(uri)?.delete() }
        }

        override suspend fun deleteUnused(keep: Set<String>) {
            withContext(Dispatchers.IO) {
                dir.listFiles()?.filter { Uri.fromFile(it).toString() !in keep }?.forEach(File::delete)
            }
        }

        private fun writeJpeg(bitmap: Bitmap): String {
            val file = File(dir, "bg_${UUID.randomUUID()}.jpg")
            try {
                file.outputStream().use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) throw IOException("Compress failed")
                }
            } catch (e: IOException) {
                file.delete()
                throw e
            } finally {
                bitmap.recycle()
            }
            return Uri.fromFile(file).toString()
        }

        /** Only files inside our folder may be read or deleted. */
        private fun ownedFile(uri: String): File? {
            val path = uri.toUri().takeIf { it.scheme == "file" }?.path ?: return null
            val file = File(path).canonicalFile
            return file.takeIf { it.parentFile == dir.canonicalFile }
        }

        private fun decodeScaled(uri: Uri): Bitmap =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // ImageDecoder also applies EXIF rotation and handles HEIC.
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    val scale = minOf(1f, MAX_SIDE.toFloat() / max(info.size.width, info.size.height))
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: throw IOException("Cannot decode $uri")
            }

        private companion object {
            const val MAX_SIDE = 1600
            const val JPEG_QUALITY = 85
        }
    }
