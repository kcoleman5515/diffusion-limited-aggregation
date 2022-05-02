/*
 *  Copyright 2022 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.dla.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import edu.cnm.deepdive.dla.model.Direction
import java.util.*

class LatticeView : View {

    var onSeedListener: ((LatticeView, Int, Int) -> Unit)? = null
    var rng = Random()
    var size = 0
        set(value) {
            field = value
            bitmap = Bitmap
                .createBitmap(size, size, Bitmap.Config.ARGB_8888)
                .apply {
                    eraseColor(Color.BLACK)
                }
            source[0, 0, size] = size
            lattice?.also {
                drawOnBitmap(it)
                postInvalidate()
            }
        }
    var lattice: BitSet? = null
        set(value) {
            bitmap?.let {
                if (field != null) {
                    field = value?.let {
                        field!!.xor(it)
                        drawOnBitmap(field!!)
                        postInvalidate()
                        it.clone() as BitSet
                    }
                } else {
                    field = value?.let {
                        drawOnBitmap(it)
                        postInvalidate()
                        it.clone() as BitSet
                    }
                }
            }
        }

    private val source = Rect()
    private val dest = Rect()
    private var bitmap: Bitmap? = null
    private var startX = 0
    private var startY = 0

    constructor(context: Context?) : super(context) {}

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    constructor(
        context: Context?, attrs: AttributeSet?, defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = resolveSizeAndState(
            paddingLeft + paddingRight + suggestedMinimumWidth, widthMeasureSpec, 0
        )
        val height = resolveSizeAndState(
            paddingTop + paddingBottom + suggestedMinimumHeight, heightMeasureSpec, 0
        )
        val size = Math.max(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = Math.round(event.x)
                startY = Math.round(event.y)
                handled = true
            }
            MotionEvent.ACTION_UP -> if (event.eventTime - event.downTime < LONG_PRESS_TIMEOUT) {
                val deltaX = Math.round(event.x) - startX
                val deltaY = Math.round(event.y) - startY
                if (deltaX * deltaX + deltaY * deltaY < MAX_CLICK_TRAVEL_SQUARED) {
                    handled = true
                    performClick()
                }
            }
            else -> {
                handled = super.onTouchEvent(event)
            }
        }
        return handled
    }

    override fun performClick(): Boolean {
        super.performClick()
        onSeedListener?.invoke(this, startX * size / width, startY * size / width)
        return true
    }

    override fun onDraw(canvas: Canvas) {
        bitmap?.let {
            dest.set(0, 0, width, height)
            canvas.drawBitmap(it, source, dest, null)
        }
    }


    fun clear() {
        lattice = null
        size = size
    }

    private fun drawOnBitmap(lattice: BitSet) {
        var offset = lattice.nextSetBit(0)
        while (offset > -1) {
            val x = offset % size
            val y = offset / size
            drawPixel(x, y)
            offset = lattice.nextSetBit(offset + 1)
        }
    }

    private fun drawPixel(x: Int, y: Int) {
        val hsv = floatArrayOf(0f, 1f, 1f)
        @ColorInt var color: Int
        var neighborHue = getNeighborHue(x, y)
        if (neighborHue < 0) {
            neighborHue = rng.nextInt(MAX_HUE).toFloat()
        } else {
            neighborHue += (rng.nextInt(3) - 1).toFloat()
            neighborHue %= MAX_HUE.toFloat()
            if (neighborHue < 0) {
                neighborHue += MAX_HUE.toFloat()
            }
        }
        hsv[0] = neighborHue
        bitmap?.setPixel(x, y, Color.HSVToColor(hsv))
    }

    private fun getNeighborHue(x: Int, y: Int): Float {
        val hsv = floatArrayOf(0f, 1f, 1f)
        var neighborCount = 0
        var neighborHue = -1f
        for (d in Direction.values()) {
            val neighborX = x + d.offsetX
            val neighborY = y + d.offsetY
            if (neighborX >= 0 && neighborX < size && neighborY >= 0 && neighborY < size) {
                @ColorInt val pixel = bitmap!!.getPixel(neighborX, neighborY)
                if (pixel != Color.BLACK
                    && (++neighborCount == 1 || rng.nextInt(neighborCount) == 0)
                ) {
                    Color.colorToHSV(pixel, hsv)
                    neighborHue = hsv[0]
                }
            }
        }
        return neighborHue
    }


    companion object {
        private val LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout()
        private const val MAX_CLICK_TRAVEL_SQUARED = 20
        private const val MAX_HUE = 360
    }
}