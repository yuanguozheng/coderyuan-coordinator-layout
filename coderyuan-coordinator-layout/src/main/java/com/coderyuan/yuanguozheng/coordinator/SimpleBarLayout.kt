package com.coderyuan.yuanguozheng.coordinator

import android.content.Context
import android.support.v4.view.NestedScrollingChild
import android.support.v4.view.NestedScrollingChildHelper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout

/**
 * 头部View
 * 基本抄的AppBarLayout，主要功能是把嵌套手势抛给NestedScrollingParent
 * 包括捕获基本的滑动手势，和Fling速度
 */
class SimpleBarLayout : FrameLayout, NestedScrollingChild {

    private var velocityTracker: VelocityTracker? = null

    private val childHelper: NestedScrollingChildHelper by lazy(LazyThreadSafetyMode.NONE) {
        NestedScrollingChildHelper(this)
    }

    constructor(context: Context) : super(context) {
        setupViews()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setupViews()
    }

    private fun setupViews() {
        childHelper.isNestedScrollingEnabled = true
    }

    private var lastMotionY = 0

    private val consumed = IntArray(2)
    private val offset = IntArray(2)

    private var nestedOffsetY = 0

    private var consumedFling = false

    private val touchSlop by lazy(LazyThreadSafetyMode.NONE) { ViewConfiguration.get(context).scaledTouchSlop }

    private var isBeingDragged: Boolean = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSpec = View.MeasureSpec.makeMeasureSpec((1 shl 30) - 1, View.MeasureSpec.UNSPECIFIED)
        super.onMeasure(widthMeasureSpec, heightSpec)
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return childHelper.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        childHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return childHelper.hasNestedScrollingParent()
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false
        val action = ev.action

        if (action == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isBeingDragged = false
                val y = ev.y.toInt()
                lastMotionY = y
                ensureVelocityTracker()
            }

            MotionEvent.ACTION_MOVE -> {
                val y = ev.y.toInt()
                val yDiff = Math.abs(y - lastMotionY)
                if (yDiff > touchSlop) {
                    isBeingDragged = true
                    lastMotionY = y
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isBeingDragged = false
                if (velocityTracker != null) {
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
            }
        }

        if (velocityTracker != null) {
            velocityTracker?.addMovement(ev)
        }

        return isBeingDragged
    }

    private fun ensureVelocityTracker() {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false
        val result = super.onTouchEvent(ev)
        if (result) {
            return true
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val y = ev.y.toInt()
                nestedOffsetY = 0
                lastMotionY = y
                ensureVelocityTracker()
            }

            MotionEvent.ACTION_MOVE -> {
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                val y = ev.y.toInt()
                var dy = lastMotionY - y

                if (dispatchNestedPreScroll(0, dy, consumed, offset)) {
                    dy -= consumed[1]
                    ev.offsetLocation(0F, (-consumed[1]).toFloat())
                    nestedOffsetY += offset[1]
                }

                if (!isBeingDragged && Math.abs(dy) > touchSlop) {
                    isBeingDragged = true
                    if (dy > 0) {
                        dy -= touchSlop
                    } else {
                        dy += touchSlop
                    }
                }

                if (isBeingDragged) {
                    lastMotionY = y - offset[1]
                    if (dispatchNestedScroll(0, 0, 0, dy, offset)) {
                        ev.offsetLocation(0f, offset[1].toFloat())
                        lastMotionY -= offset[1]
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (velocityTracker != null) {
                    velocityTracker?.addMovement(ev)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val yvel = (velocityTracker?.yVelocity ?: 0f) * -1
                    if (dispatchNestedPreFling(0f, yvel)) {
                        dispatchNestedFling(0f, yvel, consumedFling)
                    }
                }
                isBeingDragged = false
                if (velocityTracker != null) {
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
                stopNestedScroll()
            }
            MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                if (velocityTracker != null) {
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
                stopNestedScroll()
            }
        }

        if (velocityTracker != null) {
            velocityTracker?.addMovement(ev)
        }
        return true
    }
}