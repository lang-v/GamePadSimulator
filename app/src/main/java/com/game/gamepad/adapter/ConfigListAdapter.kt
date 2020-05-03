package com.game.gamepad.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.game.gamepad.R
import com.game.gamepad.config.ConfigItem

class ConfigListAdapter(
    context: Context,
    private val itemList: ArrayList<ConfigItem>,
    private val resources: Int,
    private val multiple: Boolean
) :
    ArrayAdapter<ConfigItem>(context, resources) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = itemList[position]
        val view = LayoutInflater.from(context).inflate(resources, parent, false)
        view.setOnClickListener {
            select(position)
        }
        val title = view.findViewById<TextView>(R.id.chooseConfigName)
        val radio = view.findViewById<RadioButton>(R.id.chooseConfigRadio)
        title.text = item.name
        radio.isChecked = item.selected
        return view
    }

    private fun select(position: Int) {
        //单选
        if (!multiple) {
            //已选中
            if (itemList[position].selected) return
            itemList[position].selected = true
            //只选中一项
            for (i in 0 until count) {
                if (i != position) {
                    itemList[i].selected = false
                }
            }
        }else{
            //改变选中状态
            itemList[position].selected = !itemList[position].selected
        }
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return itemList.size
    }

    fun getSelected():ArrayList<ConfigItem>{
        val array = ArrayList<ConfigItem>()
        for (configItem in itemList) {
            if (configItem.selected)
                array.add(configItem)
        }
        return array
    }
}
