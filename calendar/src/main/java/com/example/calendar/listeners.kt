package com.example.calendar

import java.util.*

interface OnDateSelectedListener {
    fun onDateSelected(dayCalendar: Calendar, events: List<CalendarEvent>?)
}

interface OnLoadEventsListener {
    fun onLoadEvents(year: Int, month: Int): List<CalendarEvent>?
}

interface OnMonthChangedListener {
    fun onMonthChanged(monthCalendar: Calendar)
}
