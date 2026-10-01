package com.chibychibystore.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

/**
 * Bridges calendar-day values between Material 3's date picker and the
 * instants persisted for a promotion period.
 *
 * The picker reports a selection as **UTC midnight of the chosen calendar
 * date**, so `Date(millis)` is a UTC day marker, not a local instant. A
 * promotion period is stored as the *UTC* boundaries of that day
 * ([startOfUtcDay] / [endOfUtcDay]): the values depend only on the picked
 * calendar date, not on the device zone at save time, so a period means the
 * same day everywhere and never shifts if the device zone later changes.
 *
 * The active-promotion query compares full timestamps, so it must open the day
 * at its *UTC* start (see [startOfUtcDay]) — not a local start-of-day, which on
 * a device west of UTC would fall before the stored boundary and drop the
 * promotion on its final day.
 *
 * The invariant is:
 *
 *  - the day the user picked is the day that is stored and matched, in every
 *    zone and after any later zone change;
 *  - a period stored with [startOfUtcDay] / [endOfUtcDay] brackets a UTC
 *    query boundary from [startOfUtcDay] of the same day.
 */
object CalendarDates {

    /** The UTC calendar date a picker day-marker stands for. */
    fun utcDay(date: Date): LocalDate =
        Instant.ofEpochMilli(date.time).atZone(ZoneOffset.UTC).toLocalDate()

    /** A picker day-marker (UTC midnight) for [day]. */
    fun utcDayMarker(day: LocalDate): Date =
        Date(day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())

    /** First instant of the picker day [date], expressed in UTC. */
    fun startOfUtcDay(date: Date): Date =
        Date(utcDay(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())

    /** Last instant of the picker day [date], expressed in UTC. */
    fun endOfUtcDay(date: Date): Date {
        val nextDayStart = utcDay(date).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return Date(nextDayStart - 1)
    }
}
