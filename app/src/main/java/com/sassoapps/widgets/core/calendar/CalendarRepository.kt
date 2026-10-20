package com.sassoapps.widgets.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
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
        context.contentResolver.query(uri, projection, selection, null, "${Instances.BEGIN} ASC")
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
    }

    /** All-day events are stored as UTC midnights; shift them to local midnight of the same date. */
    private fun utcMidnightToLocal(utcMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(utcMillis).atOffset(ZoneOffset.UTC).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    companion object {
        /** Opens the event in the user's calendar app. */
        fun viewEventIntent(event: CalendarEvent): Intent =
            Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end)

        /** Opens the default calendar app at the current time. */
        fun openCalendarIntent(now: Long = System.currentTimeMillis()): Intent =
            Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").also { ContentUris.appendId(it, now) }.build())
    }
}
