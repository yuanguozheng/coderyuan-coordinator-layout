package com.coderyuan.customcoordinator

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_simple.btn_fold
import kotlinx.android.synthetic.main.activity_simple.btn_unfold
import kotlinx.android.synthetic.main.activity_simple.coordinator_layout
import kotlinx.android.synthetic.main.activity_simple.hover_view
import kotlinx.android.synthetic.main.activity_simple.view_pager

class SimpleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple)

        val rv1 = RecyclerView(this)
        val linearLayoutManager = LinearLayoutManager(this)
        rv1.layoutManager = linearLayoutManager
        rv1.adapter = TestAdapter(resources.getColor(android.R.color.holo_green_light))
        rv1.adapter?.notifyDataSetChanged()

        val rv2 = RecyclerView(this)
        val linearLayoutManager2 = LinearLayoutManager(this)
        rv2.layoutManager = linearLayoutManager2
        rv2.adapter = TestAdapter(resources.getColor(android.R.color.holo_green_dark))
        rv2.adapter?.notifyDataSetChanged()

        val viewList = listOf(rv1, rv2)
        val pagerAdapter = MyPagerAdapter(viewList)
        view_pager?.adapter = pagerAdapter
        view_pager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                hover_view?.text = "我是悬停View 当前是第${p0 + 1}页"
            }
        })

        coordinator_layout?.headerBarLayoutId = R.id.bar_layout
        coordinator_layout?.hoverLayoutId = R.id.hover_view
        coordinator_layout?.contentLayoutId = R.id.view_pager
        coordinator_layout?.initViewsAfterSetIds()
        coordinator_layout?.currentScrollingViewCallback = {
            viewList[view_pager.currentItem]
        }

        btn_fold?.setOnClickListener {
            coordinator_layout?.foldHeader()
        }

        btn_unfold?.setOnClickListener {
            coordinator_layout?.unfoldHeader()
        }
    }

    inner class TestAdapter(private val bgColor: Int) : RecyclerView.Adapter<TestViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
            val v = LayoutInflater.from(this@SimpleActivity).inflate(R.layout.test_item, parent, false)
            v.setBackgroundColor(bgColor)
            return TestViewHolder(v)
        }

        override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
            holder.textView?.text = position.toString()
        }

        override fun getItemCount(): Int {
            return 100
        }
    }

    inner class TestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textView: TextView? = null

        init {
            textView = itemView.findViewById(R.id.tv)
        }

    }
}