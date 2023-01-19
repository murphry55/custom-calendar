package com.example.calendar

import android.util.SparseArray
import java.util.*

private const val DEFAULT_DAYS_IN_WEEK = 7

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val amountOfDays: Int,
    val firstWeekDay: Int,
    val calendar: Calendar
) {
    private var events: SparseArray<ArrayList<CalendarEvent>>? = null

    fun getEventOfDay(dayOfMonth: Int): List<CalendarEvent>? {
        return events?.get(dayOfMonth)
    }

    fun setEvents(events: SparseArray<ArrayList<CalendarEvent>>) {
        this.events = events
    }

    fun getDayIndex(dayOfMonth: Int): Int {
        return DEFAULT_DAYS_IN_WEEK + firstWeekDay + dayOfMonth
    }

    fun compareByDate(calendarMonth: CalendarMonth): Boolean {
        return year == calendarMonth.year && month == calendarMonth.month
    }
}
