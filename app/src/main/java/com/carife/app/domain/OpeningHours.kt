package com.carife.app.domain

import com.carife.app.core.model.Cafe
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object OpeningHours {

    val BEKASI_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    private val MAKASSAR_ZONE: ZoneId = ZoneId.of("Asia/Makassar")

    private val DAY_TOKENS = mapOf(
        "Mo" to DayOfWeek.MONDAY,
        "Tu" to DayOfWeek.TUESDAY,
        "We" to DayOfWeek.WEDNESDAY,
        "Th" to DayOfWeek.THURSDAY,
        "Fr" to DayOfWeek.FRIDAY,
        "Sa" to DayOfWeek.SATURDAY,
        "Su" to DayOfWeek.SUNDAY,
    )

    private val DAY_PREFIX = Regex(
        "^((?:(?:Mo|Tu|We|Th|Fr|Sa|Su)(?:\\s*-\\s*(?:Mo|Tu|We|Th|Fr|Sa|Su))?(?:\\s*,\\s*)?)+)\\s*(.*)$"
    )
    private val TIME_RANGE = Regex("^(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})$")
    private val EVERY_DAY_AND_HOLIDAYS = Regex("^Mo\\s*-\\s*Su\\s*,\\s*PH(?=\\s)")

    data class Interval(val startMinute: Int, val endMinute: Int)

    data class Parsed(val intervalsByDay: Map<DayOfWeek, List<Interval>>) {
        fun isOpenAt(moment: Instant, zone: ZoneId = BEKASI_ZONE): Boolean {
            val local = LocalDateTime.ofInstant(moment, zone)
            val minutes = local.hour * 60 + local.minute
            return intervalsByDay[local.dayOfWeek].orEmpty().any { minutes >= it.startMinute && minutes < it.endMinute }
        }
    }

    fun isAlwaysOpen(raw: String?): Boolean =
        raw?.trim()?.equals("24/7", ignoreCase = true) == true

    fun parse(raw: String?): Parsed? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (text.equals("24/7", ignoreCase = true)) {
            return Parsed(DayOfWeek.entries.associateWith { listOf(Interval(0, 1440)) })
        }

        val result = linkedMapOf<DayOfWeek, MutableList<Interval>>()

        for (rule in text.split(';')) {
            val trimmed = rule.trim().replace(EVERY_DAY_AND_HOLIDAYS, "Mo-Su")
            if (trimmed.isEmpty()) continue

            val days: List<DayOfWeek>
            val timePart: String
            val prefixMatch = DAY_PREFIX.find(trimmed)
            if (prefixMatch != null) {
                days = parseDays(prefixMatch.groupValues[1]) ?: return null
                timePart = prefixMatch.groupValues[2].trim()
            } else {
                days = DayOfWeek.entries.toList()
                timePart = trimmed
            }

            val ranges = when (timePart.lowercase()) {
                "closed", "off" -> emptyList()
                "24/7" -> listOf(0 to 1440)
                else -> parseTimeRanges(timePart)?.takeIf { it.isNotEmpty() } ?: return null
            }

            // A later weekly rule replaces earlier hours on its listed days.
            days.forEach { day -> result[day] = mutableListOf() }
            for (day in days) {
                for ((start, end) in ranges) {
                    val bucket = result.getOrPut(day) { mutableListOf() }
                    if (end > start) {
                        bucket.add(Interval(start, end))
                    } else {
                        bucket.add(Interval(start, 1440))
                        result.getOrPut(day.plus(1)) { mutableListOf() }.add(Interval(0, end))
                    }
                }
            }
        }

        return if (result.isEmpty()) null else Parsed(result)
    }

    private fun parseDays(spec: String): List<DayOfWeek>? {
        val days = mutableListOf<DayOfWeek>()
        for (token in spec.split(',')) {
            val part = token.trim()
            if (part.isEmpty()) continue
            val dashSplit = part.split('-')
            if (dashSplit.size == 2) {
                val start = DAY_TOKENS[dashSplit[0].trim()] ?: return null
                val end = DAY_TOKENS[dashSplit[1].trim()] ?: return null
                var current = start
                var guard = 0
                while (guard < 8) {
                    days.add(current)
                    if (current == end) break
                    current = current.plus(1)
                    guard++
                }
            } else {
                val day = DAY_TOKENS[part] ?: return null
                days.add(day)
            }
        }
        return if (days.isEmpty()) null else days.distinct()
    }

    private fun parseTimeRanges(spec: String): List<Pair<Int, Int>>? {
        val ranges = mutableListOf<Pair<Int, Int>>()
        for (chunk in spec.split(',')) {
            val part = chunk.trim()
            if (part.isEmpty()) continue
            val match = TIME_RANGE.find(part) ?: return null
            val startHour = match.groupValues[1].toIntOrNull() ?: return null
            val startMinute = match.groupValues[2].toIntOrNull() ?: return null
            val endHour = match.groupValues[3].toIntOrNull() ?: return null
            val endMinute = match.groupValues[4].toIntOrNull() ?: return null
            if (startHour > 23 || endHour > 24 || startMinute > 59 || endMinute > 59) return null
            if (endHour == 24 && endMinute != 0) return null
            ranges.add(startHour * 60 + startMinute to endHour * 60 + endMinute)
        }
        return ranges
    }

    fun isOpenNow(raw: String?, now: Instant = Instant.now()): Boolean? =
        parse(raw)?.isOpenAt(now)

    fun isOpenNow(cafe: Cafe, now: Instant): Boolean? {
        val zone = if (cafe.cityId == "makassar") MAKASSAR_ZONE else BEKASI_ZONE
        return parse(cafe.openingHoursRaw)?.isOpenAt(now, zone)
    }
}
