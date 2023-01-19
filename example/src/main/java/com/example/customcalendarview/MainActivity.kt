package com.example.customcalendarview

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import com.example.calendar.CalendarEvent
import com.example.calendar.OnLoadEventsListener
import com.example.calendar.OnMonthChangedListener
import com.example.customcalendarview.databinding.ActivityMainBinding
import com.example.customcalendarview.utils.CalendarMonthNameFormatter
import java.util.*

/**
 * Простой пример использования календаря из модуля :calendar
 */

class MainActivity : Activity(), OnMonthChangedListener,
    OnLoadEventsListener {

    private lateinit var binding: ActivityMainBinding

    private val formatter = CalendarMonthNameFormatter(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.calendarView.apply {
            setOnMonthChangedListener(this@MainActivity)
            setOnLoadEventsListener(this@MainActivity)
        }
    }

    override fun onMonthChanged(monthCalendar: Calendar) {
        binding.textMonth.text = formatter.format(monthCalendar)
    }

    override fun onLoadEvents(year: Int, month: Int): List<CalendarEvent> {
        val events = ArrayList<CalendarEvent>()
        val calendar = Calendar.getInstance()
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        val daysNumbs = getRandomNumbs(
            1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH),
            10
        )
        for (dayNumb in daysNumbs) {
            calendar[Calendar.DAY_OF_MONTH] = dayNumb
            val event = CalendarEvent(calendar.timeInMillis, getRandomColor())
            events.add(event)
        }
        return events
    }

    private fun getRandomNumbs(start: Int, end: Int, count: Int): IntArray {
        return IntArray(count) { start + (Math.random() * end).toInt() }
    }

    private fun getRandomColor() = when ((Math.random() * 4).toInt()) {
        0 -> Color.RED
        1 -> Color.YELLOW
        2 -> Color.BLUE
        3 -> Color.GREEN
        else -> Color.BLACK
    }
}
