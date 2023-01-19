package com.example.calendar.utils

import java.util.*

fun Calendar.getFirstWeekDayOfMonth(): Int {
    val utilCalendar = clone() as Calendar
    utilCalendar[Calendar.DAY_OF_MONTH] = 1
    val dayOfWeek = utilCalendar[Calendar.DAY_OF_WEEK] - utilCalendar.firstDayOfWeek
    return if (dayOfWeek < 0) 7 + dayOfWeek else dayOfWeek
}

fun Calendar.getYear(): Int {
    return this[Calendar.YEAR]
}

fun Calendar.getMonth(): Int {
    return this[Calendar.MONTH]
}

fun Calendar.getDayOfMonth(): Int {
    return this[Calendar.DAY_OF_MONTH]
}

fun Calendar.getNumberOfDaysInMonth(): Int {
    return this.getActualMaximum(Calendar.DAY_OF_MONTH)
}

fun getTodayCalendar(): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = System.currentTimeMillis()
    return calendar
}

fun isSameMonth(calendar: Calendar, calendar2: Calendar): Boolean {
    return calendar.getYear() == calendar2.getYear() && calendar.getMonth() == calendar2.getMonth()
}

fun Calendar.setNextMonth() {
    add(Calendar.MONTH, 1)
}

fun Calendar.setPreviousMonth() {
    add(Calendar.MONTH, -1)
}
