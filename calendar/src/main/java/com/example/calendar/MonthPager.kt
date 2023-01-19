package com.example.calendar

import android.util.SparseArray
import com.example.calendar.utils.*
import java.util.*
import kotlin.collections.ArrayList

class MonthPager(private var firstDayOfWeek: Int) {

    private var reachedMax = false
    private var reachedMin = false

    private var selectedDay = 0

    private var minDate: Calendar? = null
    private var maxDate: Calendar? = null

    private var currentMonthCalendar: Calendar = getTodayCalendar()

    private var previousMonth: CalendarMonth? = null
    private var focusedMonth: CalendarMonth? = null
    private var nextMonth: CalendarMonth? = null

    private var onLoadEventsListener: OnLoadEventsListener? = null

    init {
        currentMonthCalendar.firstDayOfWeek = firstDayOfWeek
        val focusedMonthCalendar = currentMonthCalendar.clone() as Calendar
        selectedDay = focusedMonthCalendar.getDayOfMonth()
        setCalendarMonths(focusedMonthCalendar)
    }

    private fun setCalendarMonths(focusedMonthCalendar: Calendar) {
        if (minDate != null && currentMonthCalendar.timeInMillis < minDate!!.timeInMillis) {
            setCalendarMonths(minDate!!)
            invalidateRange()
            return
        } else if (maxDate != null && currentMonthCalendar.timeInMillis > maxDate!!.timeInMillis) {
            setCalendarMonths(maxDate!!)
            invalidateRange()
            return
        }
        val nextMonthCalendar = focusedMonthCalendar.clone() as Calendar
        nextMonthCalendar.setNextMonth()
        val previousMonthCalendar = focusedMonthCalendar.clone() as Calendar
        previousMonthCalendar.setPreviousMonth()
        previousMonth = buildCalendarMonth(previousMonthCalendar)
        focusedMonth = buildCalendarMonth(focusedMonthCalendar)
        nextMonth = buildCalendarMonth(nextMonthCalendar)
    }

    private fun buildCalendarMonth(calendar: Calendar): CalendarMonth {
        val month = CalendarMonth(
            year = calendar.getYear(),
            month = calendar.getMonth(),
            firstWeekDay = calendar.getFirstWeekDayOfMonth(),
            amountOfDays = calendar.getNumberOfDaysInMonth(),
            calendar = calendar
        )
        loadEventsForMonth(month)
        return month
    }

    private fun loadEventsForMonth(calendarMonth: CalendarMonth) {
        val monthEvents: List<CalendarEvent> = onLoadEventsListener?.onLoadEvents(
            calendarMonth.year,
            calendarMonth.month
        ) ?: return

        val eventsByDay = SparseArray<ArrayList<CalendarEvent>>()
        val eventCalendar = Calendar.getInstance()
        for (calendarEvent in monthEvents) {
            eventCalendar.timeInMillis = calendarEvent.timeInMillis
            if (isSameMonth(eventCalendar, calendarMonth.calendar)) {
                val dayOfMonth: Int = eventCalendar.getDayOfMonth()
                var eventsOfDay: ArrayList<CalendarEvent>? = eventsByDay[dayOfMonth]
                if (eventsOfDay == null) {
                    eventsOfDay = ArrayList()
                    eventsOfDay.add(calendarEvent)
                    eventsByDay.put(dayOfMonth, eventsOfDay)
                } else {
                    eventsOfDay.add(calendarEvent)
                }
            }
        }
        calendarMonth.setEvents(eventsByDay)
    }

    private fun invalidateRange() {
        if (maxDate != null && isSameMonth(
                calendar = getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar,
                calendar2 = maxDate!!
            )
        ) {
            reachedMax = true
        }
        if (minDate != null && isSameMonth(
                calendar = getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar,
                calendar2 = minDate!!
            )
        ) {
            reachedMin = true
        }
    }

    fun goForward() {
        selectDay(1)
        previousMonth = focusedMonth
        focusedMonth = nextMonth

        val calendar = focusedMonth?.calendar?.clone() as? Calendar ?: return
        calendar.setNextMonth()
        nextMonth = buildCalendarMonth(calendar)
        if (maxDate != null && isSameMonth(
                calendar = getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar,
                calendar2 = maxDate!!
            )
        ) {
            reachedMax = true
        }
        if (reachedMin) {
            reachedMin = false
        }
    }

    fun goBack() {
        selectDay(1)
        nextMonth = focusedMonth
        focusedMonth = previousMonth

        val calendar = focusedMonth?.calendar?.clone() as? Calendar ?: return
        calendar.setPreviousMonth()
        previousMonth = buildCalendarMonth(calendar)
        if (minDate != null && isSameMonth(
                calendar = getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar,
                calendar2 = minDate!!
            )
        ) {
            reachedMin = true
        }
        if (reachedMax) {
            reachedMax = false
        }
    }

    fun getCalendarMonth(monthIndex: MonthIndex): CalendarMonth {
        return when (monthIndex) {
            MonthIndex.PREVIOUS_MONTH -> previousMonth!!
            MonthIndex.FOCUSED_MONTH -> focusedMonth!!
            MonthIndex.NEXT_MONTH -> nextMonth!!
        }
    }

    fun isReachedMax(): Boolean {
        return reachedMax
    }

    fun isReachedMin(): Boolean {
        return reachedMin
    }

    fun getSelectedDay(): Int {
        return selectedDay
    }

    fun getCurrentDay(): Int {
        return currentMonthCalendar.getDayOfMonth()
    }

    fun isOnCurrentMonth(monthIndex: MonthIndex): Boolean {
        return getCalendarMonth(monthIndex).year == currentMonthCalendar.getYear() &&
                getCalendarMonth(monthIndex).month == currentMonthCalendar.getMonth()
    }

    fun selectDay(day: Int) {
        selectedDay = day
    }

    fun setFirstDayOfWeek(day: Int) {
        firstDayOfWeek = day
        val focusedMonthCalendar: Calendar = getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar
        focusedMonthCalendar.firstDayOfWeek = firstDayOfWeek
        setCalendarMonths(focusedMonthCalendar)
    }

    fun setOnLoadEventsListener(listener: OnLoadEventsListener?) {
        onLoadEventsListener = listener

        loadEventsForMonth(getCalendarMonth(MonthIndex.PREVIOUS_MONTH))
        loadEventsForMonth(getCalendarMonth(MonthIndex.FOCUSED_MONTH))
        loadEventsForMonth(getCalendarMonth(MonthIndex.NEXT_MONTH))
    }

    fun setMinimumDate(timeInMillis: Long) {
        minDate = Calendar.getInstance().apply {
            firstDayOfWeek = this@MonthPager.firstDayOfWeek
            setTimeInMillis(timeInMillis)
        }
        setCalendarMonths(currentMonthCalendar)
    }

    fun setMaximumDate(timeInMillis: Long) {
        maxDate = Calendar.getInstance().apply {
            firstDayOfWeek = this@MonthPager.firstDayOfWeek
            setTimeInMillis(timeInMillis)
        }
        setCalendarMonths(currentMonthCalendar)
    }

    fun updateEvents() {
        loadEventsForMonth(getCalendarMonth(MonthIndex.PREVIOUS_MONTH))
        loadEventsForMonth(getCalendarMonth(MonthIndex.FOCUSED_MONTH))
        loadEventsForMonth(getCalendarMonth(MonthIndex.NEXT_MONTH))
    }
}
