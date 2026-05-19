package com.balaji.callhistory.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Provides **cached, thread-local** [SimpleDateFormat] instances.
 *
 * [SimpleDateFormat] is expensive to construct and NOT thread-safe.
 * Thread-local storage gives each thread its own instance, so we avoid
 * both the construction cost and synchronization overhead.
 *
 * Replaces the repeated `SimpleDateFormat(…)` calls in:
 *  - [CallHistoryPagingSource]
 *  - [CallHistoryDetailsViewModel]
 *  - [UiHelper]
 */
object DateFormatterHelper {

    // Format pattern constants
    private const val PATTERN_DATE_HEADER     = "dd MMM yyyy"
    private const val PATTERN_TIME_WITH_DATE  = "hh:mm a dd-MMM-yy"
    private const val PATTERN_DAY             = "EEEE"
    private const val PATTERN_SHORT_DATE      = "MMM dd"
    private const val PATTERN_SHORT_DATE_YEAR = "MMM dd, yy"
    private const val PATTERN_DATE_COMPARE    = "yyyyMMdd"
    private const val PATTERN_DATE_FULL       = "MMMM dd, yy"

    // Thread-local formatters
    private val fmtDateHeader     = threadFmt(PATTERN_DATE_HEADER)
    private val fmtTimeWithDate   = threadFmt(PATTERN_TIME_WITH_DATE)
    private val fmtDay            = threadFmt(PATTERN_DAY)
    private val fmtShortDate      = threadFmt(PATTERN_SHORT_DATE)
    private val fmtShortDateYear  = threadFmt(PATTERN_SHORT_DATE_YEAR)
    private val fmtDateCompare    = threadFmt(PATTERN_DATE_COMPARE)
    private val fmtDateFull       = threadFmt(PATTERN_DATE_FULL)

    /** "dd MMM yyyy" — used as grouped date header in call list. */
    fun formatDateHeader(timestamp: Long): String =
        fmtDateHeader.get()!!.format(Date(timestamp))

    /** "hh:mm a dd-MMM-yy" — full time + date for detail screens. */
    fun formatTimeWithDate(timestamp: Long): String =
        fmtTimeWithDate.get()!!.format(Date(timestamp))

    /** "EEEE" — day-of-week name used by day filter. */
    fun formatDayName(timestamp: Long): String =
        fmtDay.get()!!.format(Date(timestamp))

    /** "hh:mm a  MMM dd" — compact time + date used for list rows. */
    fun formatDateAndTime(timestamp: Long): String {
        val d = Date(timestamp)
        return "${fmtShortDate.get()!!.format(d)}  ${fmtShortDate.get()!!.format(d)}"
    }

    /** "hh:mm a • MMM dd, yy" — time + date with year separator. */
    fun formatDateTimeWithYear(timestamp: Long): String {
        val d = Date(timestamp)
        return "${fmtShortDate.get()!!.format(d)} • ${fmtShortDateYear.get()!!.format(d)}"
    }

    /**
     * Returns a "yyyyMMdd" string for the given [timestamp], used to
     * compare dates when building section headers.
     */
    fun comparableDate(timestamp: Long): String =
        fmtDateCompare.get()!!.format(Date(timestamp))

    /** Returns a long-form date string for non-today/yesterday dates. */
    fun formatFullDate(timestamp: Long): String =
        fmtDateFull.get()!!.format(Date(timestamp))

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun threadFmt(pattern: String): ThreadLocal<SimpleDateFormat> =
        ThreadLocal.withInitial { SimpleDateFormat(pattern, Locale.getDefault()) }
}

