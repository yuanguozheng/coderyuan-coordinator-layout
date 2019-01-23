package com.coderyuan.yuanguozheng.coordinator

import android.content.Context
import android.support.v4.view.NestedScrollingParent
import android.support.v4.view.NestedScrollingParentHelper
import android.support.v4.view.ScrollingView
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.Scroller

class SimpleCoordinatorLayout : LinearLayout, NestedScrollingParent {

    // 直接用属性了，懒得写attr
    var headerBarLayoutId = 0

    var hoverLayoutId = 0

    var contentLayoutId = 0

    /**
     * 获取当前正在滑动的View（RecyclerView/ListView/ScrollView）
     */
    var currentScrollingViewCallback: (() -> View?)? = null

    constructor(context: Context?) : super(context) {
        setupViews()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setupViews()
    }

    private fun setupViews() {
        this.orientation = VERTICAL
    }

    /**
     * 头部BarView，必须是一个SimpleBarLayout
     */
    private var headerBarLayout: SimpleBarLayout? = null

    /**
     * 悬停状态的顶部View，任意View都可以
     */
    private var hoverView: View? = null

    /**
     * 底部包含列表的父级View，如果没有使用ViewPager这样的控件，有可能直接是列表View本身
     */
    private var contentView: View? = null

    /**
     * 根据指定好的Id来获取相关的View
     */
    fun initViewsAfterSetIds() {
        headerBarLayout = findViewById(headerBarLayoutId)
        hoverView = findViewById(hoverLayoutId)
        contentView = findViewById(contentLayoutId)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        contentView?.layoutParams?.height = this.measuredHeight - (hoverView?.measuredHeight ?: 0)
        contentView?.requestLayout()
    }

    /**
     * 最大滚动距离
     */
    private var maxScrollY: Int = 0
        get() {
            if (field == 0) {
                field = (headerBarLayout?.height ?: 0) - (hoverView?.height ?: 0)
            }
            return field
        }

    /**
     * 用于处理父级嵌套滑动Layout
     */
    private val parentHelper: NestedScrollingParentHelper by lazy(LazyThreadSafetyMode.NONE) { NestedScrollingParentHelper(this) }

    /**
     * 用于处理Fling手势
     */
    private val scroller: Scroller by lazy(LazyThreadSafetyMode.NONE) { Scroller(context) }

    /**
     * 收起头部View
     */
    fun foldHeader() {
        scrollTo(0, maxScrollY)
    }

    /**
     * 展开头部View
     */
    fun unfoldHeader() {
        scrollTo(0, 0)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        parentHelper.onNestedScrollAccepted(child, target, axes)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        scroller.abortAnimation()
        return true
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        // barLayout不处理，由它抛出来并处理onNestedScroll
        if (target is SimpleBarLayout) {
            return
        }
        // 到顶了，还往上拉，当前View（父级）不消耗距离，全部交给NestedScrollingChild
        if ((scrollY == 0 && dy < 0)) {
            consumed[1] = 0
            return
        }
        // 已经到最大滑动值（悬停态），还往下拉，当前View（父级）不消耗距离，全部交给NestedScrollingChild
        if (scrollY == maxScrollY && dy > 0) {
            consumed[1] = 0
            return
        }
        // 由列表触发的向上嵌套滑动，而列表还没滑到顶部，当前View（父级）不消耗距离，全部交给NestedScrollingChild
        if (target is ScrollingView && target.computeVerticalScrollOffset() != 0 && dy < 0) {
            consumed[1] = 0
            return
        }
        // 计算父级View需要消耗的Y轴距离
        val targetDy = when {
            // 向上拉，马上就要到顶了，剩余的距离小于产生的滑动距离，消耗剩余的距离
            (dy < 0 && scrollY + dy < 0) -> {
                0 - scrollY
            }
            // 向下拉，马上就要到悬停态了，剩余的距离小于产生的滑动距离，消耗剩余的距离
            (dy > 0 && scrollY + dy > maxScrollY) -> {
                maxScrollY - scrollY
            }
            // 其他情况，全部消耗掉
            else -> {
                dy
            }
        }
        // 进行消耗
        consumed[1] = targetDy
        scrollBy(0, targetDy)
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        // 处理由bar产生的嵌套滑动
        if (target is SimpleBarLayout) {
            scrollBy(0, dyUnconsumed)
        }
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        var minY = 0
        // 如果是向上拉的手势，而且还处于悬浮状态（即Header已收起）
        // 那么可滑动的Y最小值，相对RecyclerView来说，需要包含一个最大可收缩距离，以进行连贯滑动
        // 其他情况，最小就为0，最大直接设置为Int.MAX，滑不超的
        if (velocityY < 0 && scrollY == maxScrollY) {
            minY = -maxScrollY
        }
        // 起始点需要根据收缩距离和列表滑动的距离算
        val startY = scrollY + getListVerticalScrollOffset()
        doFling(velocityY.toInt(), startY, minY)
        return true
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        // 处理所有Fling手势，禁止RecyclerView自己Fling，以便连贯滑动
        return true
    }

    override fun onStopNestedScroll(target: View) {
        parentHelper.onStopNestedScroll(target)
    }

    /**
     * 调用Scroller处理Fling手势
     *
     * @param velocityY Y轴速度
     * @param startY 起始Y
     * @param minY 最小Y值
     */
    private fun doFling(velocityY: Int, startY: Int, minY: Int) {
        scroller.fling(0, startY, 0, velocityY, 0, 0, minY, Int.MAX_VALUE)
        invalidate()
    }

    /**
     * 上次滑动的ScrollY，用于计算dY，从而使用scrollBy（RecyclerView不支持scrollTo）
     */
    private var lastScrollY = 0

    /**
     * 用于计算Fling的scrollY
     */
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            if (lastScrollY == 0) {
                lastScrollY = scroller.currY
            }
            val y = scroller.currY
            val dy = y - lastScrollY

            if (scrollY == maxScrollY && dy > 0) {
                // 已经到了悬停状态，还进行下翻手势，此时由列表进行滚动
                currentScrollingViewCallback?.invoke()?.scrollBy(0, dy)
            } else {
                if (getListVerticalScrollOffset() != 0) {
                    // 如果列表没有滑动到最顶部，那么由列表内部进行滚动
                    currentScrollingViewCallback?.invoke()?.scrollBy(0, dy)
                } else {
                    // 列表已经到了顶部，滚动外面的View，即：将Header滑出来，或者收进去
                    scrollBy(0, dy)
                }
            }
            lastScrollY = y
            // 注意：不要使用postInvalidate，否则会导致scroller计算来不及，导致抖动
            invalidate()
        } else {
            lastScrollY = 0
        }
    }

    override fun scrollTo(x: Int, y: Int) {
        // 限制一下当前View的Scroll范围：0~maxScroll，超出maxScroll不应该接受滚动
        val targetY: Int = when {
            y < 0 -> {
                0
            }
            y > maxScrollY -> {
                maxScrollY
            }
            else -> {
                y
            }
        }
        super.scrollTo(0, targetY)
    }

    /**
     * 获取列表View当前的滑动offset（直接取ScrollY恒为零）
     */
    private fun getListVerticalScrollOffset(): Int {
        return (currentScrollingViewCallback?.invoke() as? ScrollingView)?.computeVerticalScrollOffset() ?: 0
    }
}