package com.alexxx.coolviews

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs


class MyView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val pic = ContextCompat.getDrawable(context, R.drawable.my_view)

    private val rectHolder: Rect = Rect()

    private var time: Int
    private val align: Int
    private val speed: Int

    init {
        val a: TypedArray = context.obtainStyledAttributes(
            attrs, R.styleable.MyView, 0, 0
        )
        try {
            val clockwise = a.getBoolean(R.styleable.MyView_clockwise, true)
            val specifiedSpeed = a.getInt(R.styleable.MyView_speed, 2)
            time = 0
            speed = if (clockwise) specifiedSpeed else -specifiedSpeed
            align = a.getInt(R.styleable.MyView_align, 1)
        } finally {
            a.recycle()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        pic?.let { pic ->
            canvas?.let {
                time = (time + speed) % 720

                val picW = pic.intrinsicWidth
                val picH = pic.intrinsicHeight

                var viewWidth = width
                var viewHeight = height

                val scl = picW / picH
                if (scl > viewWidth / viewHeight) {
                    viewHeight = viewWidth / scl
                } else {
                    viewWidth = viewHeight * scl
                }

                val gapW = width - viewWidth
                val gapH = height - viewHeight

                val left = gapW * align / 2
                val top = gapH * align / 2

                pic.setBounds(left, top, left + viewWidth, top + viewHeight)

                it.save()

                drawFirstPic(it, left, top, viewWidth, viewHeight, pic)

                it.restore()

                drawSecondPic(it, left, top, viewWidth, viewHeight, pic)
            }
        }

        invalidate()
    }

    private fun drawFirstPic(
        it: Canvas,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        pic: Drawable
    ) {
        if (abs(time) < 360) {
            it.rotate(
                time.toFloat(),
                (left + width / 4).toFloat(),
                (top + height / 2).toFloat()
            )
        }
        rectHolder.set(left + 0, top + 0, left + width / 2, top + height)
        it.clipRect(rectHolder)
        pic.draw(it)
    }

    private fun drawSecondPic(
        it: Canvas,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        pic: Drawable
    ) {
        if (abs(time) > 360) {
            val localTime = time % 360
            val angle = (localTime / 90) * 90
            it.rotate(
                angle.toFloat(),
                (left + 3 * width / 4).toFloat(),
                (top + height / 2).toFloat()
            )
        }
        rectHolder.set(left + width / 2, top + 0, left + width, top + height)
        it.clipRect(rectHolder)
        pic.draw(it)
    }

    class MyState(state: Parcelable?, val time: Int) : BaseSavedState(state)

    override fun onSaveInstanceState(): Parcelable? {
        return MyState(super.onSaveInstanceState(), time)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val myS = state as MyState
        super.onRestoreInstanceState(myS.superState)
        time = myS.time
    }
}