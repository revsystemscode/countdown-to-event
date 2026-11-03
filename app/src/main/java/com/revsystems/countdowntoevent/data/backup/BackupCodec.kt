package com.revsystems.countdowntoevent.data.backup

import com.revsystems.countdowntoevent.domain.EventValidator
import com.revsystems.countdowntoevent.domain.model.Event
import com.revsystems.countdowntoevent.domain.model.EventBackground
import com.revsystems.countdowntoevent.domain.model.RepeatRule
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Base64

/** File format. Readable JSON so a backup can be inspected; photos travel inside as base64. */
@Serializable
data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val events: List<BackupEvent>,
)

@Serializable
data class BackupEvent(
    val title: String,
    val emoji: String? = null,
    val startDate: String,
    val startTime: String? = null,
    val endDate: String? = null,
    val timeZone: String,
    val repeat: String = RepeatRule.NONE.name,
    val background: BackupBackground = BackupBackground(type = BackupBackground.SOLID, value = "FF1A9AA6"),
)

@Serializable
data class BackupBackground(
    val type: String,
    val value: String? = null,
    @SerialName("image") val imageBase64: String? = null,
) {
    companion object {
        const val SOLID = "solid"
        const val PRESET = "preset"
        const val IMAGE = "image"
    }
}

/** An event read from a backup; its photo (if any) still has to be stored before saving. */
data class DecodedEvent(
    val event: Event,
    val image: ByteArray?,
)

data class DecodedBackup(
    val events: List<DecodedEvent>,
    /** Entries that were malformed or failed validation and were left out. */
    val invalid: Int,
)

class InvalidBackupException(
    message: String,
) : Exception(message)

object BackupCodec {
    const val FORMAT = "countdown-to-event-backup"
    const val VERSION = 1

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true // newer app versions may add fields
            explicitNulls = false
        }

    fun encode(
        events: List<Event>,
        exportedAt: Instant,
        readImage: (uri: String) -> ByteArray?,
    ): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                format = FORMAT,
                version = VERSION,
                exportedAt = exportedAt.toString(),
                events = events.map { it.toBackup(readImage) },
            ),
        )

    /** @throws InvalidBackupException if the file isn't a backup this version can read. */
    fun decode(text: String): DecodedBackup {
        val file =
            try {
                json.decodeFromString(BackupFile.serializer(), text)
            } catch (e: SerializationException) {
                throw InvalidBackupException("Not a backup file: ${e.message}")
            } catch (e: IllegalArgumentException) {
                throw InvalidBackupException("Not a backup file: ${e.message}")
            }
        if (file.format != FORMAT) throw InvalidBackupException("Unknown format ${file.format}")
        if (file.version > VERSION) throw InvalidBackupException("Backup from a newer app version (${file.version})")

        val decoded = file.events.mapNotNull { it.toDecoded() }
        return DecodedBackup(decoded, invalid = file.events.size - decoded.size)
    }

    /** Same title, date, time, length and repeat = the same event (used to skip duplicates). */
    fun identity(event: Event) = listOf(event.title.trim().lowercase(), event.startDate, event.startTime, event.endDate, event.repeat)

    private fun Event.toBackup(readImage: (String) -> ByteArray?): BackupEvent {
        val bg =
            when (val b = background) {
                is EventBackground.Solid -> BackupBackground(BackupBackground.SOLID, "%08X".format(b.argb))
                is EventBackground.Preset -> BackupBackground(BackupBackground.PRESET, b.key)
                is EventBackground.LocalImage ->
                    readImage(b.uri)?.let { BackupBackground(BackupBackground.IMAGE, imageBase64 = Base64.getEncoder().encodeToString(it)) }
                        // Photo missing on disk: keep the event with the default color.
                        ?: BackupBackground(BackupBackground.SOLID, "%08X".format((EventBackground.Default as EventBackground.Solid).argb))
            }
        return BackupEvent(
            title = title,
            emoji = emoji,
            startDate = startDate.toString(),
            startTime = startTime?.toString(),
            endDate = endDate?.toString(),
            timeZone = timeZone.id,
            repeat = repeat.name,
            background = bg,
        )
    }

    private fun BackupEvent.toDecoded(): DecodedEvent? =
        try {
            var image: ByteArray? = null
            val bg =
                when (background.type) {
                    BackupBackground.SOLID -> background.value?.toLongOrNull(16)?.let { EventBackground.Solid(it.toInt()) }
                    BackupBackground.PRESET -> background.value?.let(EventBackground::Preset)
                    BackupBackground.IMAGE -> {
                        image = background.imageBase64?.let { Base64.getDecoder().decode(it) }
                        // Placeholder until the photo is stored on this device.
                        image?.let { EventBackground.LocalImage("") }
                    }
                    else -> null
                } ?: EventBackground.Default
            val event =
                Event(
                    title = title.trim(),
                    emoji = emoji?.trim()?.ifEmpty { null },
                    startDate = LocalDate.parse(startDate),
                    startTime = startTime?.let(LocalTime::parse),
                    endDate = endDate?.let(LocalDate::parse),
                    timeZone = ZoneId.of(timeZone),
                    repeat = RepeatRule.entries.firstOrNull { it.name == repeat } ?: RepeatRule.NONE,
                    background = bg,
                )
            if (EventValidator.validate(event).isEmpty()) DecodedEvent(event, image) else null
        } catch (_: DateTimeException) {
            null
        } catch (_: IllegalArgumentException) {
            // Bad base64.
            null
        }
}
