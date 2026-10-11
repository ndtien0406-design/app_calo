package com.ndtien.calotrack.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {

    private const val ISO_PATTERN = "yyyy-MM-dd"
    private const val TIME_PATTERN = "HH:mm"
    private const val DISPLAY_PATTERN = "dd/MM/yyyy"

    private fun isoFormat() = SimpleDateFormat(ISO_PATTERN, Locale.US)
    private fun displayFormat() = SimpleDateFormat(DISPLAY_PATTERN, Locale.US)

    /** yyyy-MM-dd */
    fun today(): String = isoFormat().format(Calendar.getInstance().time)

    /** HH:mm */
    fun nowTime(): String = SimpleDateFormat(TIME_PATTERN, Locale.US).format(Calendar.getInstance().time)

    private fun parseIso(iso: String): Calendar? {
        return try {
            val date = isoFormat().parse(iso) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (_e: Exception) {
            null
        }
    }

    /** "11/10/2026" */
    fun toDisplay(iso: String): String {
        val cal = parseIso(iso) ?: return iso
        return displayFormat().format(cal.time)
    }

    /** "CN, 11/10/2026" — thứ dạng T2..T7, CN */
    fun toDisplayWithWeekday(iso: String): String {
        return "${weekdayShort(iso)}, ${toDisplay(iso)}"
    }

    /** "T2".."CN" — dùng cho chart */
    fun weekdayShort(iso: String): String {
        val cal = parseIso(iso) ?: return ""
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "T2"
            Calendar.TUESDAY -> "T3"
            Calendar.WEDNESDAY -> "T4"
            Calendar.THURSDAY -> "T5"
            Calendar.FRIDAY -> "T6"
            Calendar.SATURDAY -> "T7"
            else -> "CN"
        }
    }

    /** "Thứ 2, 12/10" — pill ngày trên app bar (kiểu bản xem thử). */
    fun todayPill(): String {
        return toPill(today())
    }

    /** "Thứ 2, 12/10" cho một ngày iso bất kỳ. */
    fun toPill(iso: String): String {
        val cal = parseIso(iso) ?: return iso
        val wd = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Thứ 2"
            Calendar.TUESDAY -> "Thứ 3"
            Calendar.WEDNESDAY -> "Thứ 4"
            Calendar.THURSDAY -> "Thứ 5"
            Calendar.FRIDAY -> "Thứ 6"
            Calendar.SATURDAY -> "Thứ 7"
            else -> "Chủ nhật"
        }
        val dm = SimpleDateFormat("dd/MM", Locale.US).format(cal.time)
        return "$wd, $dm"
    }

    /** Số ngày trong tháng của một ngày iso ("12"). */
    fun dayOfMonth(iso: String): String {
        val cal = parseIso(iso) ?: return ""
        return cal.get(Calendar.DAY_OF_MONTH).toString()
    }

    fun addDays(iso: String, n: Int): String {
        val cal = parseIso(iso) ?: return iso
        cal.add(Calendar.DAY_OF_MONTH, n)
        return isoFormat().format(cal.time)
    }

    /** 7 ngày gần nhất, tăng dần (hôm nay cuối cùng) */
    fun last7Days(): List<String> = (6 downTo 0).map { addDays(today(), -it) }

    fun isToday(iso: String): Boolean = iso == today()
}
