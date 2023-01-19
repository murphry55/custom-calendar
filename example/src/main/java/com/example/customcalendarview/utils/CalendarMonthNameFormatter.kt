package com.example.customcalendarview.utils

import android.text.format.DateFormat
import java.util.*

class CalendarMonthNameFormatter(private val showYear: Boolean) {
    fun format(calendar: Calendar): CharSequence {
        val format = "LLLL" + if (showYear) " yyyy" else ""
        val monthName = DateFormat.format(format, calendar.time).toString()
        return monthName[0].uppercaseChar().toString() +
                monthName.substring(1, monthName.length)
    }
}
