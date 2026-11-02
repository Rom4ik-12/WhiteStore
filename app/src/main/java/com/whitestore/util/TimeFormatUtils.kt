package com.whitestore.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatUtils {

    /**
     * Formats timestamp into human-readable relative time (in Russian):
     * - "только что" (less than 1 minute)
     * - "менее 1 часа назад" (1 min .. 59 min)
     * - "1 час назад", "2 часа назад", "5 часов назад" (1h .. 23h)
     * - "вчера" / "1 день назад", "2 дня назад", "5 дней назад" (1d .. 30d)
     * - "более месяца назад" (> 30d)
     */
    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Никогда"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0L || diff < 60_000L) {
            return "только что"
        }

        val minutes = diff / (60 * 1000L)
        val hours = diff / (60 * 60 * 1000L)
        val days = diff / (24 * 60 * 60 * 1000L)

        return when {
            minutes < 60 -> "менее 1 часа назад"
            hours < 24 -> {
                val rem10 = (hours % 10).toInt()
                val rem100 = (hours % 100).toInt()
                val suffix = when {
                    rem100 in 11..19 -> "часов"
                    rem10 == 1 -> "час"
                    rem10 in 2..4 -> "часа"
                    else -> "часов"
                }
                "$hours $suffix назад"
            }
            days < 30 -> {
                if (days == 1L) {
                    "1 день назад"
                } else {
                    val rem10 = (days % 10).toInt()
                    val rem100 = (days % 100).toInt()
                    val suffix = when {
                        rem100 in 11..19 -> "дней"
                        rem10 == 1 -> "день"
                        rem10 in 2..4 -> "дня"
                        else -> "дней"
                    }
                    "$days $suffix назад"
                }
            }
            else -> "более месяца назад"
        }
    }

    /**
     * Exact date format: e.g. "10:30 01.11.26"
     */
    fun formatExactDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Никогда"
        val sdf = SimpleDateFormat("HH:mm dd.MM.yy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
