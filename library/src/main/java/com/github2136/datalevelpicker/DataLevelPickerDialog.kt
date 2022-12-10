package com.github2136.datalevelpicker

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.CheckedTextView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by YB on 2022/12/5
 * 级联菜单选择
 */
class DataLevelPickerDialog private constructor() : DialogFragment(), View.OnClickListener {
    private val className by lazy { javaClass.simpleName }
    protected lateinit var dataLevel: MutableList<IDataLevel>
    private var selectData = mutableListOf<IDataLevel>() //选中的集合
    private var selectIndex = mutableListOf<Int>() //选中的集合下标
    private var level = 0 //当前操作等级
    private var onConfirm: ((data: MutableList<IDataLevel>) -> Unit)? = null
    private lateinit var hsvTitle: HorizontalScrollView
    private lateinit var llTitle: LinearLayout
    private lateinit var rvList: RecyclerView
    private lateinit var btnConfirm: TextView
    private lateinit var adapter: DataLevelPickerAdapter

    constructor(data: MutableList<IDataLevel>, onConfirm: (data: MutableList<IDataLevel>) -> Unit) : this() {
        dataLevel = data
        this.onConfirm = onConfirm
    }

    fun setData(data: MutableList<IDataLevel>) {
        selectData.clear()
        selectIndex.clear()
        level = data.lastIndex
        var list: MutableList<IDataLevel>? = dataLevel
        for (d in data) {
            list?.apply {
                val i = indexOfFirst { it.getId() == d.getId() }
                selectIndex.add(i)
                list = get(i).getChild()
                selectData.add(get(i))
            }
        }
    }

    fun show(manager: FragmentManager) {
        if (!this.isAdded) {
            show(manager, className)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.apply {
            setGravity(Gravity.BOTTOM)
            decorView.setPadding(0)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, dp2px(300f))
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }
        val view = inflater.inflate(R.layout.dialog_level_picker, container)
        hsvTitle = view.findViewById(R.id.hsvTitle)
        llTitle = view.findViewById(R.id.llTitle)
        rvList = view.findViewById(R.id.rvList)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        btnConfirm.setOnClickListener(this)
        rvList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        llTitle.post {
            var list: MutableList<IDataLevel>? = dataLevel
            selectData.forEachIndexed { i, item ->
                list?.apply {
                    val data = first { item.getId() == it.getId() }
                    val index = indexOfFirst { item.getId() == it.getId() }
                    addTitle(inflater, data, i)
                    list = get(index).getChild()
                }
            }
            setTitleCheck(llTitle, level)
            hsvTitle.postDelayed({ hsvTitle.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }, 100)
        }

        var list = dataLevel
        for ((i, index) in selectIndex.withIndex()) {
            if (level == i) {
                adapter = DataLevelPickerAdapter(list).apply { selectPosition = index }
            } else {
                list = list[i].getChild()!!
            }
        }
        if (!::adapter.isInitialized) {
            adapter = DataLevelPickerAdapter(dataLevel)
        }
        adapter.setOnItemClickListener { position ->
            val item = adapter.getItem(position)!!
            if (level >= selectData.size) {
                //添加title
                selectData.add(item)
                selectIndex.add(position)
                addTitle(inflater, item, level)
                setTitleCheck(llTitle, level)
                hsvTitle.postDelayed({ hsvTitle.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }, 100)
            } else {
                //替换
                (llTitle.getChildAt(level) as CheckedTextView).text = item.getText()
                selectData[level] = item
                selectIndex[level] = position
                val childCount = llTitle.childCount
                llTitle.removeViews(level + 1, childCount - (level + 1))
                selectData = selectData.subList(0, level + 1)
                selectIndex = selectIndex.subList(0, level + 1)
                setTitleCheck(llTitle, level)
                hsvTitle.postDelayed({ hsvTitle.fullScroll(HorizontalScrollView.FOCUS_RIGHT) }, 100)
            }
            item.getChild()?.apply {
                //展示下一级
                level++
                adapter.selectPosition = -1
                adapter.setData(this)
            } ?: let {
                //切换选中的最后一级
                adapter.selectPosition = position
                adapter.notifyDataSetChanged()
            }
        }
        rvList.adapter = adapter
        return view
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ctvTop -> {
                val clickLevel = v.tag as Int
                level = clickLevel
                if (v is CheckedTextView) {
                    if (!v.isChecked) {
                        setTitleCheck(llTitle, clickLevel)
                        var list = dataLevel
                        for ((i, index) in selectIndex.withIndex()) {
                            Log.e("TAG", "setData1: ${selectIndex.joinToString { it.toString() }}")
                            if (clickLevel == i) {
                                adapter.selectPosition = index
                                adapter.setData(list)
                            } else {
                                list = list[index].getChild()!!
                            }
                        }
                    }
                }
            }
            R.id.btnConfirm -> {
                onConfirm?.invoke(selectData.toMutableList())
                dismiss()
            }
        }
    }

    /**
     * 添加标题
     */
    private fun addTitle(inflater: LayoutInflater, item: IDataLevel, level: Int) {
        val title = inflater.inflate(R.layout.item_dlp_top, llTitle, false) as CheckedTextView
        title.isChecked = false
        title.setOnClickListener(this)
        title.text = item.getText()
        title.tag = level
        llTitle.addView(title)
    }

    /**
     * 设置指定标题显示选中
     */
    private fun setTitleCheck(llTitle: LinearLayout, level: Int) {
        llTitle.children.forEachIndexed { index, view ->
            if (view is CheckedTextView) {
                view.isChecked = view.tag == level
            }
        }
    }

    private fun dp2px(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics).toInt()
}