package com.ssajudn.hushkeep.core.common

import com.ssajudn.hushkeep.core.config.DateFormats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object HushkeepDateTimeFormatter {
    private val timelineFormatter = DateTimeFormatter.ofPattern(
        DateFormats.TIMELINE_DAY,
        Locale.getDefault(),
    )
    private val exportFormatter = DateTimeFormatter.ofPattern(
        DateFormats.EXPORT_FILE,
        Locale.ROOT,
    )
    private val timelineTimestampFormatter = DateTimeFormatter.ofPattern(
        DateFormats.TIMELINE_TIMESTAMP,
        Locale.getDefault(),
    )
    private val isoFormatter = DateTimeFormatter.ofPattern(
        DateFormats.ISO_INSTANT,
        Locale.ROOT,
    ).withZone(ZoneId.of("UTC"))

    fun timelineDay(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        timelineFormatter.withZone(zoneId).format(instant)

    fun timelineTimestamp(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        timelineTimestampFormatter.withZone(zoneId).format(instant)

    fun exportFileTimestamp(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        exportFormatter.withZone(zoneId).format(instant)

    fun isoInstant(instant: Instant): String = isoFormatter.format(instant)
}
