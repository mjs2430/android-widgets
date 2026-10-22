package com.sassoapps.widgets.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads calendars and events from the device's calendar provider (all synced accounts). */
class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun calendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val projection = arrayOf(Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME, Calendars.CALENDAR_COLOR)
        context.contentResolver.query(Calendars.CONTENT_URI, projection, "${Calendars.VISIBLE} = 1", null, null)
            ?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(CalendarInfo(id = c.getLong(0), name = c.getString(1).orEmpty(), accountName = c.getString(2).orEmpty(), color = c.getInt(3)))
                    }
                }
            }
            .orEmpty()
            .sortedWith(compareBy({ it.accountName }, { it.name }))
    }

    /** Event occurrences overlapping [from, to], limited to [calendarIds] (empty means all calendars). */
    suspend fun events(calendarIds: Set<Long>, from: Long, to: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val uri = Instances.CONTENT_URI.buildUpon()
            .also { ContentUris.appendId(it, from); ContentUris.appendId(it, to) }
            .build()
        val projection = arrayOf(
            Instances.EVENT_ID, Instances.CALENDAR_ID, Instances.TITLE, Instances.BEGIN, Instances.END,
            Instances.ALL_DAY, Instances.EVENT_LOCATION, Instances.DISPLAY_COLOR, Instances.SELF_ATTENDEE_STATUS,
        )
        var selection = "${Instances.VISIBLE} = 1"
        if (calendarIds.isNotEmpty()) {
            selection += " AND ${Instances.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
        }
        val zone = ZoneId.systemDefault()
        val instances = context.contentResolver.query(uri, projection, selection, null, "${Instances.BEGIN} ASC")
            ?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val allDay = c.getInt(5) == 1
                        add(CalendarEvent(
                            eventId = c.getLong(0),
                            calendarId = c.getLong(1),
                            title = c.getString(2)?.takeIf { it.isNotBlank() } ?: "(No title)",
                            begin = if (allDay) utcMidnightToLocal(c.getLong(3), zone) else c.getLong(3),
                            end = if (allDay) utcMidnightToLocal(c.getLong(4), zone) else c.getLong(4),
                            allDay = allDay,
                            location = c.getString(6)?.takeIf { it.isNotBlank() },
                            color = c.getInt(7),
                            declined = c.getInt(8) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
                        ))
                    }
                }
            }
            .orEmpty()
        val seen = instances.mapTo(HashSet()) { it.eventId }
        (instances + eventsMissingFromInstances(calendarIds, from, to, seen)).sortedBy { it.begin }
    }

    /**
     * Timed events starting in the window that the provider made no instance for, so [events] never sees
     * them, though calendar apps still show them: an event saved with no end time or duration, or a
     * recurring one whose rule ends before its own start. They're shown once, at their start. An event with
     * no end or duration is treated as lasting [NO_END_DURATION], so it stays "Now" a while after it starts.
     * A series like that can have an occurrence moved to another time, in which case the moved copy is the
     * real event and the original slot is skipped.
     */
    private fun eventsMissingFromInstances(calendarIds: Set<Long>, from: Long, to: Long, seen: Set<Long>): List<CalendarEvent> {
        val projection = arrayOf(
            Events._ID, Events.CALENDAR_ID, Events.TITLE, Events.DTSTART, Events.DTEND, Events.DURATION,
            Events.EVENT_LOCATION, Events.EVENT_COLOR, Events.CALENDAR_COLOR, Events.SELF_ATTENDEE_STATUS, Events.RRULE,
        )
        // Cancelled occurrences and ones removed from a series (EXDATE) are meant to be missing. So are the
        // slots of a healthy series (the provider expands those) and moved or edited copies (they have their own row).
        var selection = "${Events.DELETED} = 0 AND ${Events.VISIBLE} = 1 AND ${Events.ALL_DAY} = 0 AND " +
            "${Events.ORIGINAL_ID} IS NULL AND (${Events.RRULE} IS NULL OR ${Events.LAST_DATE} < ${Events.DTSTART}) AND " +
            "(${Events.STATUS} IS NULL OR ${Events.STATUS} != ${Events.STATUS_CANCELED}) AND " +
            "(${Events.EXDATE} IS NULL OR ${Events.EXDATE} = '') AND " +
            "${Events.DTSTART} >= ? AND ${Events.DTSTART} <= ?"
        if (calendarIds.isNotEmpty()) {
            selection += " AND ${Events.CALENDAR_ID} IN (${calendarIds.joinToString(",")})"
        }
        val args = arrayOf((from - NO_END_DURATION).toString(), to.toString())
        val missing = context.contentResolver.query(Events.CONTENT_URI, projection, selection, args, "${Events.DTSTART} ASC")
            ?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        if (c.getLong(0) in seen) continue
                        val begin = c.getLong(3)
                        val end = c.getLong(4).takeIf { it > begin }
                            ?: parseDurationMillis(c.getString(5))?.let { begin + it }
                        add(CalendarEvent(
                            eventId = c.getLong(0),
                            calendarId = c.getLong(1),
                            title = c.getString(2)?.takeIf { it.isNotBlank() } ?: "(No title)",
                            begin = begin,
                            end = end ?: (begin + NO_END_DURATION),
                            allDay = false,
                            location = c.getString(6)?.takeIf { it.isNotBlank() },
                            color = if (c.isNull(7)) c.getInt(8) else c.getInt(7),
                            declined = c.getInt(9) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
                            endKnown = end != null,
                        ) to !c.isNull(10))
                    }
                }
            }
            .orEmpty()
        // A series' slot that was moved: the moved copy records which slot it replaced.
        val series = missing.filter { (_, recurring) -> recurring }.map { it.first.eventId }
        val moved = if (series.isEmpty()) emptySet() else movedSlots(series)
        return missing.map { it.first }.filterNot { (it.eventId to it.begin) in moved }
    }

    /** (series id, start of the slot) for every copy of those series that replaced one of their slots. */
    private fun movedSlots(seriesIds: List<Long>): Set<Pair<Long, Long>> {
        val projection = arrayOf(Events.ORIGINAL_ID, Events.ORIGINAL_INSTANCE_TIME)
        val selection = "${Events.ORIGINAL_ID} IN (${seriesIds.joinToString(",")})"
        return context.contentResolver.query(Events.CONTENT_URI, projection, selection, null, null)
            ?.use { c ->
                buildSet {
                    while (c.moveToNext()) add(c.getLong(0) to c.getLong(1))
                }
            }
            .orEmpty()
    }

    /** All-day events are stored as UTC midnights; shift them to local midnight of the same date. */
    private fun utcMidnightToLocal(utcMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(utcMillis).atOffset(ZoneOffset.UTC).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    companion object {
        /** How long an event saved without an end time is treated as lasting. */
        private const val NO_END_DURATION = 60 * 60 * 1000L

        /** Opens the event in the user's calendar app. */
        fun viewEventIntent(event: CalendarEvent): Intent =
            Intent(Intent.ACTION_VIEW)
                // The type is set explicitly: launchers lack READ_CALENDAR, so they can't ask the provider for it,
                // and an untyped intent matches no calendar app (or a chooser that never remembers "Always").
                .setDataAndType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId), EVENT_MIME_TYPE)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)

        /** Opens the calendar app on the day of [now] (any time, not just the current one). */
        fun openCalendarIntent(now: Long = System.currentTimeMillis()): Intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").also { ContentUris.appendId(it, now) }.build(), TIME_MIME_TYPE)

        private const val EVENT_MIME_TYPE = "vnd.android.cursor.item/event"
        private const val TIME_MIME_TYPE = "time/epoch"
    }
}

/** An RFC 5545 duration as the provider stores it ("P3600S", "PT1H", "P1D", "P1W"), in milliseconds; null if absent or unreadable. */
internal fun parseDurationMillis(duration: String?): Long? {
    val match = Regex("""^P(?:(\d+)W)?(?:(\d+)D)?(?:T?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$""").matchEntire(duration?.trim().orEmpty()) ?: return null
    val (weeks, days, hours, minutes, seconds) = match.destructured
    fun part(text: String) = text.toLongOrNull() ?: 0L
    val total = ((part(weeks) * 7 + part(days)) * 24 + part(hours)) * 3600 + part(minutes) * 60 + part(seconds)
    return (total * 1000).takeIf { total > 0 }
}
