package com.revsystems.countdowntoevent.data.backup

import android.content.Context
import androidx.core.net.toUri
import com.revsystems.countdowntoevent.data.images.BackgroundImageStore
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.repository.EventRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(
    val imported: Int,
    val duplicates: Int,
    val invalid: Int,
)

/** Reads and writes backups through Storage Access Framework URIs picked by the user. */
@Singleton
class BackupManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val repository: EventRepository,
        private val imageStore: BackgroundImageStore,
        private val clock: Clock,
    ) {
        /** @return number of events written. */
        suspend fun export(destination: String): Int =
            withContext(Dispatchers.IO) {
                val events = repository.getEvents()
                val images = events.mapNotNull { (it.background as? EventBackground.LocalImage)?.uri }.associateWith { imageStore.read(it) }
                val text = BackupCodec.encode(events, clock.instant()) { images[it] }
                context.contentResolver.openOutputStream(destination.toUri(), "wt")?.use { it.write(text.toByteArray()) }
                    ?: throw IOException("Cannot open $destination")
                events.size
            }

        /** Adds the backup's events to the existing ones, skipping exact duplicates. */
        suspend fun import(source: String): ImportResult =
            withContext(Dispatchers.IO) {
                val text = readLimited(source)
                val backup = BackupCodec.decode(text)
                val existing = repository.getEvents().map(BackupCodec::identity).toMutableSet()
                var imported = 0
                var duplicates = 0
                var invalid = backup.invalid
                for (item in backup.events) {
                    if (!existing.add(BackupCodec.identity(item.event))) {
                        duplicates++
                        continue
                    }
                    val event =
                        if (item.image != null) {
                            val uri = runCatching { imageStore.importBytes(item.image) }.getOrNull()
                            item.event.copy(background = uri?.let(EventBackground::LocalImage) ?: EventBackground.Default)
                        } else {
                            item.event
                        }
                    runCatching { repository.save(event) }
                        .onSuccess { imported++ }
                        .onFailure { invalid++ }
                }
                ImportResult(imported, duplicates, invalid)
            }

        private fun readLimited(source: String): String {
            val input = context.contentResolver.openInputStream(source.toUri()) ?: throw IOException("Cannot open $source")
            input.use {
                val bytes = it.readNBytesCompat(MAX_BACKUP_BYTES + 1)
                if (bytes.size > MAX_BACKUP_BYTES) throw InvalidBackupException("Backup too large")
                return bytes.decodeToString()
            }
        }

        private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            while (out.size() < limit) {
                val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
                if (read < 0) break
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }

        private companion object {
            // Photos are base64 inside the JSON; 50 MB covers hundreds of events with photos.
            const val MAX_BACKUP_BYTES = 50 * 1024 * 1024
            const val BUFFER_SIZE = 64 * 1024
        }
    }
