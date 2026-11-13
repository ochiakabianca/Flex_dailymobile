package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
  private const val DATE_KEY_PATTERN = "yyyy-MM-dd"

  fun todayKey(): String = formatDateKey(Date())

  fun formatDateKey(date: Date): String {
    val sdf = SimpleDateFormat(DATE_KEY_PATTERN, Locale.US)
    return sdf.format(date)
  }

  fun parseDateKey(dateKey: String): Date? {
    return try {
      SimpleDateFormat(DATE_KEY_PATTERN, Locale.US).apply { isLenient = false }.parse(dateKey)
    } catch (_: Exception) {
      null
    }
  }

  fun recentDateKeys(daysCount: Int, anchorDateKey: String = todayKey()): List<String> {
    val anchor = parseDateKey(anchorDateKey) ?: Date()
    val cal = Calendar.getInstance()
    val result = mutableListOf<String>()
    for (offset in (daysCount - 1) downTo 0) {
      cal.time = anchor
      cal.add(Calendar.DAY_OF_YEAR, -offset)
      result.add(formatDateKey(cal.time))
    }
    return result
  }

  fun dayOfWeekShort(dateKey: String): String {
    val date = parseDateKey(dateKey) ?: return ""
    return SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase(Locale.getDefault())
  }

  fun dayOfMonthNumber(dateKey: String): String {
    val date = parseDateKey(dateKey) ?: return ""
    return SimpleDateFormat("d", Locale.getDefault()).format(date)
  }

  fun friendlyDateLabel(dateKey: String): String {
    val today = todayKey()
    if (dateKey == today) return "Today"
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    if (dateKey == formatDateKey(cal.time)) return "Yesterday"
    val date = parseDateKey(dateKey) ?: return dateKey
    return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date)
  }

  fun fullHeaderDate(dateKey: String): String {
    val date = parseDateKey(dateKey) ?: return dateKey
    return SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(date)
  }

  fun computeStreak(activeDateKeys: Set<String>, today: String = todayKey()): Int {
    if (activeDateKeys.isEmpty()) return 0
    val cal = Calendar.getInstance()
    var cursor = today
    if (!activeDateKeys.contains(cursor)) {
      cal.time = parseDateKey(today) ?: Date()
      cal.add(Calendar.DAY_OF_YEAR, -1)
      cursor = formatDateKey(cal.time)
      if (!activeDateKeys.contains(cursor)) return 0
    }
    var streak = 0
    while (activeDateKeys.contains(cursor)) {
      streak++
      val parsed = parseDateKey(cursor) ?: break
      cal.time = parsed
      cal.add(Calendar.DAY_OF_YEAR, -1)
      cursor = formatDateKey(cal.time)
    }
    return streak
  }
}
