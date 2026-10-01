package com.chibychibystore.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

/**
 * Bridges calendar-day values between Material 3's date picker and the
 * instants persisted for a promotion period.
 *
 * The picker reports a selection as **UTC midnight of the chosen calendar
 * date**, so `Date(millis)` is a UTC day marker, not a local instant. Reading it
 * back with a local `Calendar` shifts the day by the device offset: on a UTC-7
 * device, October 1 arrives as September 30 17:00 local, and "end of October 1"
 * computed locally would land on September 30 — the promotion would never apply
 * on its final day.
 *
 * Every helper here therefore interprets a picker value in UTC first and only
 * then places the day boundary in the target (local) zone. The invariant is:
 *
 *  - the day the user picked is the day that is stored and matched;
 *  - a period stored with [startOfUtcDay] / [endOfUtcDay] compares correctly
 *    against a query boundary from [startOfLocalDay].
 */
object CalendarDates {

    /** The UTC calendar date a picker day-marker stands for. */
    fun utcDay(date: Date): LocalDate =
        Instant.ofEpochMilli(date.time).atZone(ZoneOffset.UTC).toLocalDate()

    /** A picker day-marker (UTC midnight) for [day]. */
    fun utcDayMarker(day: LocalDate): Date =
        Date(day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())

    /** The local calendar date of a stored instant. */
    fun localDay(date: Date, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(date.time).atZone(zone).toLocalDate()

    /** First instant of the picker day [date], expressed in [zone]. */
    fun startOfUtcDay(date: Date, zone: ZoneId = ZoneId.systemDefault()): Date =
        Date(utcDay(date).atStartOfDay(zone).toInstant().toEpochMilli())

    /** Last instant of the picker day [date], expressed in [zone]. */
    fun endOfUtcDay(date: Date, zone: ZoneId = ZoneId.systemDefault()): Date {
        val nextDayStart = utcDay(date).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return Date(nextDayStart - 1)
    }

    /** First instant of [date]'s local calendar day — a query lower bound. */
    fun startOfLocalDay(date: Date, zone: ZoneId = ZoneId.systemDefault()): Date =
        Date(localDay(date, zone).atStartOfDay(zone).toInstant().toEpochMilli())
}
