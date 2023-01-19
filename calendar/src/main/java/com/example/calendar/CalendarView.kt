package com.example.calendar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import com.example.calendar.utils.getWeekDaysAbbreviation
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val RATIO_ROW_HEIGHT_WIDTH = 0.098f
private const val RATIO_WIDTH_PADDING_X = 12.0f
private const val RATIO_WIDTH_PADDING_Y = 15.0f
private const val RATIO_WIDTH_TEXT_HEIGHT = 36.0f
private const val RATIO_WIDTH_CIRCLE_RADIUS = 27.0f
private const val RATIO_DURATION_DISTANCE = 0.75f

private const val VELOCITY_THRESHOLD = 2000

private const val DEFAULT_DAYS_IN_WEEK = 7

private const val RESIZE_ANIMATION_DURATION = 200L

class CalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    private var offset = 0

    private var paddingX = 0f
    private var paddingY = 0f

    private var betweenX = 0f
    private var betweenY = 0f

    private var viewHeight = 0
    private var rows = 0

    private val textRect = Rect()

    private var weekDayNames: Array<String> = emptyArray()

    private var isResize = false

    private var firstDayOfWeekNumber = 0

    private var backgroundColor = 0
    private var textColor = 0
    private var textInsideCircleColor = 0
    private var weekDaysNamesColor = 0
    private var currentDayCircleColor = 0
    private var selectedDayCircleColor = 0

    private var onDateSelected: OnDateSelectedListener? = null
    private var onMonthChanged: OnMonthChangedListener? = null

    private var detector: GestureDetectorCompat
    private var scroller: OverScroller
    private var velocityTracker: VelocityTracker? = null

    private var textInsideCirclePaint: Paint
    private var textPaint: Paint
    private var textHeight = 0f
    private var selectedDayCirclePaint: Paint
    private var currentDayCirclePaint: Paint
    private var circleRadius = 0f
    private var eventCirclePaint: Paint
    private var backgroundPaint: Paint
    private var weekDaysNamesTextPaint: Paint
    private var eventCircleRadius = 0f
    private var placeForPointsWidth = 0f

    private var monthPager: MonthPager

    init {
        if (attrs != null) {
            val typedArray = context.theme.obtainStyledAttributes(
                attrs, R.styleable.CalendarView, 0, 0
            )
            try {
                backgroundColor = typedArray.getColor(
                    R.styleable.CalendarView_backgroundColor,
                    Color.WHITE
                )
                textColor = typedArray.getColor(R.styleable.CalendarView_textColor, Color.BLACK)
                textInsideCircleColor = typedArray.getColor(
                    R.styleable.CalendarView_textInsideCircleColor,
                    Color.WHITE
                )
                weekDaysNamesColor = typedArray.getColor(
                    R.styleable.CalendarView_weekDaysNamesColor,
                    Color.GRAY
                )
                currentDayCircleColor = typedArray.getColor(
                    R.styleable.CalendarView_currentDayCircleColor,
                    Color.BLACK
                )
                selectedDayCircleColor = typedArray.getColor(
                    R.styleable.CalendarView_selectedCircleColor,
                    Color.LTGRAY
                )
                firstDayOfWeekNumber =
                    typedArray.getInt(R.styleable.CalendarView_firstDayOfWeek, Calendar.MONDAY)
            } finally {
                typedArray.recycle()
            }
        }

        monthPager = MonthPager(firstDayOfWeekNumber)
        scroller = OverScroller(context)
        detector = GestureDetectorCompat(context, object : SimpleOnGestureListener() {
            override fun onDown(motionEvent: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
                if (!scroller.isFinished) {
                    return true
                }
                val calendarMonth: CalendarMonth = monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH)
                val x = motionEvent.x
                val y = motionEvent.y
                val day = getDayNumberOfCrd(x, y, calendarMonth.firstWeekDay)
                if (day < 1 || day > calendarMonth.amountOfDays) {
                    return true
                }
                monthPager.selectDay(day)
                invalidate()
                dispatchOnDateSelected(
                    calendarMonth.calendar,
                    calendarMonth.getEventOfDay(day)
                )
                return true
            }

            override fun onScroll(
                motionEvent: MotionEvent,
                motionEvent1: MotionEvent,
                dx: Float,
                dy: Float
            ): Boolean {
                parent.requestDisallowInterceptTouchEvent(true)
                val width = width
                if (dx > 0 && monthPager.isReachedMax() ||
                    dx < 0 && monthPager.isReachedMin()
                ) {
                    return true
                }
                offset -= dx.toInt()
                if (offset > width) {
                    offset = width
                } else if (offset < -width) {
                    offset = -width
                }
                invalidate()
                return true
            }

            override fun onLongPress(motionEvent: MotionEvent) {}
            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return false
            }
        })
        rows = getMonthRowsCount(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH))
        weekDayNames = getWeekDaysAbbreviation(firstDayOfWeekNumber)

        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
        }
        textInsideCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textInsideCircleColor
        }

        selectedDayCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = selectedDayCircleColor
        }

        currentDayCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = currentDayCircleColor
        }
        eventCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        weekDaysNamesTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = weekDaysNamesColor
            typeface = Typeface.DEFAULT_BOLD
        }

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = backgroundColor
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        drawMonth(canvas, MonthIndex.FOCUSED_MONTH)
        if (offset > 0) {
            drawMonth(canvas, MonthIndex.PREVIOUS_MONTH)
        }
        if (offset < 0) {
            drawMonth(canvas, MonthIndex.NEXT_MONTH)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = paddingLeft + paddingRight + suggestedMinimumWidth
        val width = resolveSizeAndState(minWidth, widthMeasureSpec, 1)
        val height: Int
        if (isResize) {
            height = viewHeight
        } else {
            height = (width * RATIO_ROW_HEIGHT_WIDTH *
                    getMonthRowsCount(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH))).toInt()
            paddingX = width / RATIO_WIDTH_PADDING_X
            paddingY = width / RATIO_WIDTH_PADDING_Y
            betweenX =
                (width - paddingX * 2) / (DEFAULT_DAYS_IN_WEEK - 1)
            betweenY = (height / rows * 6 - paddingY * 2) / 5
            textHeight =
                width / RATIO_WIDTH_TEXT_HEIGHT
            circleRadius =
                width / RATIO_WIDTH_CIRCLE_RADIUS
            eventCircleRadius = circleRadius / 7
            placeForPointsWidth = betweenX / 2
            textPaint.textSize = textHeight
            textInsideCirclePaint.textSize = textHeight
            weekDaysNamesTextPaint.textSize = textHeight
        }
        setMeasuredDimension(width, height)
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                } else {
                    velocityTracker!!.clear()
                }
                velocityTracker!!.addMovement(motionEvent)
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker!!.addMovement(motionEvent)
                velocityTracker!!.computeCurrentVelocity(1000)
            }
            MotionEvent.ACTION_UP -> {
                parent.requestDisallowInterceptTouchEvent(false)
                velocityTracker!!.computeCurrentVelocity(1000)
                handleGesture(velocityTracker!!.xVelocity)
                velocityTracker!!.recycle()
                velocityTracker!!.clear()
                velocityTracker = null
            }
        }
        return detector.onTouchEvent(motionEvent) || super.onTouchEvent(motionEvent)
    }

    private fun getDayNumberOfCrd(x: Float, y: Float, firstDayOfWeek: Int): Int {
        var x1 = x
        var y1 = y
        weekDaysNamesTextPaint.getTextBounds(
            weekDayNames[0],
            0,
            weekDayNames[0].length,
            textRect
        )
        val weekDaysNamesHeight = textRect.top + betweenY
        val widthPerDay: Float = (width - paddingX * 2 + betweenX) / DEFAULT_DAYS_IN_WEEK
        val heightPerDay = (height - paddingY * 2 - weekDaysNamesHeight + betweenY) / (rows - 1)
        x1 = x1 - paddingX + betweenX
        y1 = y1 - paddingY + betweenY - weekDaysNamesHeight
        val row = (x1 / widthPerDay).roundToInt()
        val column = (y1 / heightPerDay).roundToInt()
        return (column - 1) * DEFAULT_DAYS_IN_WEEK + row - firstDayOfWeek
    }

    private fun calculateCrdForIndex(
        index: Int,
        text: String?,
        monthIndex: MonthIndex
    ): FloatArray {
        val rowIndex: Int = (index - 1) % DEFAULT_DAYS_IN_WEEK
        val column: Int = (index - 1) / DEFAULT_DAYS_IN_WEEK
        var x = paddingX + betweenX * rowIndex + width * monthIndex.index
        var y = paddingY + betweenY * column
        x += offset.toFloat()

        if (text != null) {
            textPaint.getTextBounds(text, 0, text.length, textRect)
            x -= textRect.centerX().toFloat()
            y -= textRect.centerY().toFloat()
        }
        return floatArrayOf(x, y)
    }

    private fun drawMonth(canvas: Canvas, monthIndex: MonthIndex) {
        val calendarMonth: CalendarMonth = monthPager.getCalendarMonth(monthIndex)

        if (monthIndex == MonthIndex.FOCUSED_MONTH) {
            val crdCircle = calculateCrdForIndex(
                calendarMonth.getDayIndex(monthPager.getSelectedDay()),
                null, monthIndex
            )
            canvas.drawCircle(
                crdCircle[0], crdCircle[1], circleRadius,
                selectedDayCirclePaint
            )
        }

        if (monthPager.isOnCurrentMonth(monthIndex)) {
            val crdCircle = calculateCrdForIndex(
                calendarMonth.getDayIndex(monthPager.getCurrentDay()),
                null, monthIndex
            )
            canvas.drawCircle(
                crdCircle[0], crdCircle[1], circleRadius,
                currentDayCirclePaint
            )
        }

        for (i in 1..DEFAULT_DAYS_IN_WEEK) {
            val crd = calculateCrdForIndex(i, weekDayNames[i - 1], monthIndex)
            canvas.drawText(
                weekDayNames[i - 1], crd[0], crd[1],
                weekDaysNamesTextPaint
            )
        }

        for (day in 1..calendarMonth.amountOfDays) {
            val index = calendarMonth.getDayIndex(day)
            val crd = calculateCrdForIndex(index, day.toString(), monthIndex)
            val isCurrentDay =
                monthPager.isOnCurrentMonth(monthIndex) && day == monthPager.getCurrentDay()
            val isSelectedDay = monthIndex == MonthIndex.FOCUSED_MONTH && day == monthPager.getSelectedDay()
            canvas.drawText(
                day.toString(),
                crd[0],
                crd[1],
                (if (isCurrentDay || isSelectedDay) textInsideCirclePaint else textPaint)
            )

            val events = calendarMonth.getEventOfDay(day)
            if (events != null && !isCurrentDay && !isSelectedDay) {
                drawEventsOfDay(canvas, events, crd, day)
            }
        }
    }

    private fun drawEventsOfDay(
        canvas: Canvas,
        events: List<CalendarEvent>,
        crd: FloatArray,
        day: Int
    ) {
        val dayText = day.toString()
        textPaint.getTextBounds(dayText, 0, dayText.length, textRect)
        val offsetForCenter = placeForPointsWidth / 2 - textRect.centerX()

        val betweenPoints = placeForPointsWidth / (events.size + 1)
        for (i in events.indices) {
            eventCirclePaint.color = events[i].color
            canvas.drawCircle(
                crd[0] - offsetForCenter + betweenPoints * (i + 1),
                crd[1] + circleRadius / 2, eventCircleRadius, eventCirclePaint
            )
        }
    }

    private fun handleGesture(velocity: Float) {
        if (velocity == 0f && offset == 0) {
            return
        }
        if ((velocity > VELOCITY_THRESHOLD || offset > width / 2) && !monthPager.isReachedMin()) {
            if (!canGoBack()) {
                handleGesture(0f)
                return
            }
            monthPager.goBack()
            val distance = width - offset

            offset -= width
            scroller.startScroll(
                offset,
                0,
                distance,
                0,
                (abs(distance) * RATIO_DURATION_DISTANCE).toInt()
            )
            dispatchOnMonthChanged(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar)
            resizeView(getMonthRowsCount(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH)))
            ViewCompat.postInvalidateOnAnimation(this)
        } else if ((velocity < -VELOCITY_THRESHOLD || offset < -width / 2) && !monthPager.isReachedMax()) {
            if (!canGoForward()) {
                handleGesture(0f)
                return
            }
            monthPager.goForward()
            val distance = -width - offset

            offset += width
            scroller.startScroll(
                offset,
                0,
                distance,
                0,
                (abs(distance) * RATIO_DURATION_DISTANCE).toInt()
            )
            dispatchOnMonthChanged(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar)
            resizeView(getMonthRowsCount(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH)))
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            val distance = -offset
            scroller.startScroll(
                offset,
                0,
                distance,
                0,
                (abs(distance) * RATIO_DURATION_DISTANCE * 2).toInt()
            )
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private fun canGoForward(): Boolean {
        return offset <= 0
    }

    private fun canGoBack(): Boolean {
        return offset >= 0
    }

    override fun computeScroll() {
        super.computeScroll()
        if (scroller.computeScrollOffset()) {
            offset = scroller.currX
            invalidate()
            if (offset == scroller.finalX) {
                scroller.forceFinished(true)
                val calendarMonth: CalendarMonth = monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH)
                dispatchOnDateSelected(
                    calendarMonth.calendar,
                    calendarMonth.getEventOfDay(monthPager.getSelectedDay())
                )
            }
        }
    }

    private fun getMonthRowsCount(calendarMonth: CalendarMonth): Int {
        val rowsCount = (calendarMonth.amountOfDays + calendarMonth.firstWeekDay).toFloat() / DEFAULT_DAYS_IN_WEEK
        return ceil(rowsCount.toDouble()).toInt() + 1
    }

    private fun resizeView(targetRowsCount: Int) {
        if (rows == targetRowsCount) {
            return
        }
        class ResizeAnimation(
            private val mView: View,
            private val mTargetHeight: Int,
            private val mStartHeight: Int
        ) : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val newHeight = (mStartHeight + (mTargetHeight - mStartHeight) * interpolatedTime).toInt()
                viewHeight = newHeight
                mView.layoutParams.height = newHeight
                mView.requestLayout()
                if (interpolatedTime == 1.0f) {
                    isResize = false
                }
            }
        }

        val resizeAnimation = ResizeAnimation(
            this,
            height * targetRowsCount / rows, height
        )
        resizeAnimation.duration = RESIZE_ANIMATION_DURATION
        startAnimation(resizeAnimation)
        isResize = true
        rows = targetRowsCount
    }

    private fun dispatchOnDateSelected(calendar: Calendar, eventsOfDay: List<CalendarEvent>?) {
        onDateSelected?.onDateSelected(calendar, eventsOfDay)
    }

    private fun dispatchOnMonthChanged(calendar: Calendar) {
        onMonthChanged?.onMonthChanged(calendar)
    }

    fun updateEvents() {
        monthPager.updateEvents()
    }

    fun setFirstDayOfWeek(dayOfWeek: Int) {
        require(!(dayOfWeek < 1 || dayOfWeek > 7))
        firstDayOfWeekNumber = dayOfWeek
        weekDayNames = getWeekDaysAbbreviation(firstDayOfWeekNumber)
        monthPager.setFirstDayOfWeek(dayOfWeek)
        invalidate()
    }

    fun setMinimumDate(timeInMillis: Long) {
        monthPager.setMinimumDate(timeInMillis)
        dispatchOnMonthChanged(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar)
        invalidate()
    }

    fun setMaximumDate(timeInMillis: Long) {
        monthPager.setMaximumDate(timeInMillis)
        dispatchOnMonthChanged(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar)
        invalidate()
    }

    fun setOnDateSelectedListener(onDateSelectedListener: OnDateSelectedListener) {
        onDateSelected = onDateSelectedListener
        val calendarMonth: CalendarMonth = monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH)
        dispatchOnDateSelected(
            calendarMonth.calendar,
            calendarMonth.getEventOfDay(monthPager.getSelectedDay())
        )
    }

    fun setOnMonthChangedListener(onMonthChangedListener: OnMonthChangedListener) {
        onMonthChanged = onMonthChangedListener
        dispatchOnMonthChanged(monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar)
    }

    fun setOnLoadEventsListener(onLoadEventsListener: OnLoadEventsListener?) {
        monthPager.setOnLoadEventsListener(onLoadEventsListener)
        val calendarMonth: CalendarMonth = monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH)
        dispatchOnDateSelected(
            calendarMonth.calendar,
            calendarMonth.getEventOfDay(monthPager.getSelectedDay())
        )
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        backgroundPaint.color = backgroundColor
        invalidate()
    }

    fun getBackgroundColor(): Int {
        return backgroundColor
    }

    fun setTextColor(color: Int) {
        textColor = color
        textPaint.color = textColor
        invalidate()
    }

    fun getTextColor(): Int {
        return textColor
    }

    fun setTextInsideCircleColor(color: Int) {
        textInsideCircleColor = color
        textInsideCirclePaint.color = textInsideCircleColor
        invalidate()
    }

    fun getTextInsideCircleColor(): Int {
        return textInsideCircleColor
    }

    fun setWeekDaysNamesColor(color: Int) {
        weekDaysNamesColor = color
        weekDaysNamesTextPaint.color = weekDaysNamesColor
        invalidate()
    }

    fun getWeekDaysNamesColor(): Int {
        return weekDaysNamesColor
    }

    fun setCurrentDayCircleColor(color: Int) {
        currentDayCircleColor = color
        currentDayCirclePaint.color = currentDayCircleColor
        invalidate()
    }

    fun getCurrentDayCircleColor(): Int {
        return currentDayCircleColor
    }

    fun setSelectedDayCircleColor(color: Int) {
        selectedDayCircleColor = color
        selectedDayCirclePaint.color = selectedDayCircleColor
        invalidate()
    }

    fun getSelectedDayCircleColor(): Int {
        return selectedDayCircleColor
    }

    fun getFocusedMonthCalendar(): Calendar {
        return monthPager.getCalendarMonth(MonthIndex.FOCUSED_MONTH).calendar
    }
}
