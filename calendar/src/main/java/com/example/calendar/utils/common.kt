package com.example.calendar.utils

import java.text.DateFormatSymbols
import java.util.*

fun getWeekDaysAbbreviation(firstDayOfWeek: Int): Array<String> {
    require(!(firstDayOfWeek < 1 || firstDayOfWeek > 7))
    val dateFormatSymbols = DateFormatSymbols(Locale.getDefault())
    val shortWeekdays = dateFormatSymbols.shortWeekdays
    val weekDaysFromSunday = arrayOf(
        shortWeekdays[1], shortWeekdays[2],
        shortWeekdays[3], shortWeekdays[4], shortWeekdays[5], shortWeekdays[6],
        shortWeekdays[7]
    )
    val weekDaysNames = Array(7) { "" }
    var day = firstDayOfWeek - 1
    var i = 0
    while (i < 7) {
        day = if (day >= 7) 0 else day
        weekDaysNames[i] = weekDaysFromSunday[day].uppercase(Locale.getDefault())
        i++
        day++
    }
    return weekDaysNames
}
