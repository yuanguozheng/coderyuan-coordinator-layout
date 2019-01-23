package com.coderyuan.customcoordinator

import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup

class MyPagerAdapter(list: List<RecyclerView>) : PagerAdapter() {

    private val viewList = mutableListOf<RecyclerView>()

    init {
        viewList.addAll(list)
    }

    override fun isViewFromObject(p0: View, p1: Any): Boolean {
        return p0 == p1
    }

    override fun getCount(): Int {
        return viewList.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        container.addView(viewList[position], ViewGroup.LayoutParams(-1, -1))
        return viewList[position]
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        super.destroyItem(container, position, `object`)
        container.removeView(viewList[position])
    }
}